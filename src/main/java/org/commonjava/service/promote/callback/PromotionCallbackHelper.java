/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.service.promote.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.client.HttpClients;
import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.model.AbstractPromoteResult;
import org.commonjava.service.promote.model.CallbackTarget;
/*
import org.commonjava.util.jhttpc.HttpFactoryIfc;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;
import static org.commonjava.service.promote.model.CallbackTarget.CallbackMethod.POST;
import static org.commonjava.service.promote.model.CallbackTarget.CallbackMethod.PUT;

/**
 * Created by ruhan on 12/10/18.
 */
@ApplicationScoped
public class PromotionCallbackHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final int MAX_RETRY = 8;

/*
 * TODO: we are going to use Infinispan API directly to get rid of dep on Indy.
 *
    @Inject
    private CacheProducer cacheProducer;
*/

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    PromoteConfig config;

/*
    private SiteConfig siteConfig;

    @Inject
    private HttpFactoryIfc httpFactory;
*/

//    private CacheHandle<String, CallbackJob> retryCache;

    public PromotionCallbackHelper()
    {
    }

    @PostConstruct
    private void init()
    {
/*
        this.siteConfig = new SiteConfigBuilder().withRequestTimeoutSeconds( 30 ).withUri( config.callbackUri() ).build();
        this.retryCache = cacheProducer.getCache( PROMOTE_CALLBACK_JOB );
        this.retryCache.executeCache( (cache) -> {
            cache.addListener( new ExpireListener() );
            return null;
        } );
*/
    }

    public <T extends AbstractPromoteResult> T callback( CallbackTarget target, T ret )
    {
        return (T) callback( new CallbackJob( target, ret ) );
    }

    private <T extends AbstractPromoteResult> T callback( CallbackJob<T> job )
    {
        CallbackTarget target = job.getTarget();
        T ret = job.getRet();

        CloseableHttpClient client = null;
        try
        {
            boolean isCallbackSuccess;
            client = HttpClients.createDefault(); //httpFactory.createClient(siteConfig); // TODO: any better way to create it?
            if ( target.getMethod() == POST )
            {
                HttpPost req = new HttpPost( target.getUrl() );
                isCallbackSuccess = execute( client, req, target, ret );
            }
            else if ( target.getMethod() == PUT )
            {
                HttpPut req = new HttpPut( target.getUrl() );
                isCallbackSuccess = execute( client, req, target, ret );
            }
            else
            {
                throw new IllegalArgumentException( target.getMethod() + " not supported" );
            }

            if ( !isCallbackSuccess )
            {
                logger.warn( "Callback failed and stash it, target: {}", target );
                stash( job );
            }
        }
        catch ( Exception e )
        {
            logger.error( "Callback failed", e );
            stash( job );
        }
        finally
        {
            closeQuietly( client );
        }
        return ret;
    }

/*
    @Listener
    class ExpireListener
    {
        @CacheEntryExpired
        public void onExpired( CacheEntryExpiredEvent<String, CallbackJob> event )
        {
            if ( event.isPre() )
            {
                return;
            }
            CallbackJob job = event.getValue();
            logger.info( "Cache entry {} expired and retry, target: {}", job.getId(), job.getTarget() );
            callback( job );
        }

    }
*/

    // stash and try later
    private <T extends AbstractPromoteResult> void stash( CallbackJob job )
    {
/*
        if ( job.getRetryCount() >= MAX_RETRY )
        {
            logger.warn( "Callback failed after {} retries.", job.getRetryCount() );
            return;
        }

        int exponentialBackoff = getExponentialBackoff( job.getRetryCount() );
        job.increaseRetryCount();
        logger.debug( "Add {} to retry cache, retryCount: {}", job.getId(), job.getRetryCount() );
        retryCache.put( job.getId(), job, exponentialBackoff, TimeUnit.MINUTES );
*/
    }

    /* MAX_RETRY-1 is 7. Two to the power of 7 is 128. We get backoff series of 1, 2, 4, 8, 16, 32, 64, 128 min. */
    private int getExponentialBackoff( int retryCount )
    {
        return new Double( Math.pow( 2, retryCount ) ).intValue();
    }

    private <T extends AbstractPromoteResult> boolean execute(CloseableHttpClient client, HttpEntityEnclosingRequestBase req, CallbackTarget target, T ret )
                    throws IOException
    {
        addHeadersAndSetEntity( req, target, ret );
        CloseableHttpResponse response = client.execute( req );
        return isCallbackOk( response.getStatusLine().getStatusCode() );
    }

    private <T extends AbstractPromoteResult> void addHeadersAndSetEntity( HttpEntityEnclosingRequestBase req, CallbackTarget target, T ret )
                    throws IOException
    {
        Map<String, String> headers = target.getHeaders();
        if ( headers != null )
        {
            for ( String key : headers.keySet() )
            {
                req.setHeader( key, headers.get( key ) );
            }
        }
        req.addHeader( "Content-Type", "application/json" );
        req.setEntity( new StringEntity( objectMapper.writeValueAsString( ret ) ) );
    }

    private boolean isCallbackOk( int statusCode )
    {
        return statusCode == 200;
    }

}
