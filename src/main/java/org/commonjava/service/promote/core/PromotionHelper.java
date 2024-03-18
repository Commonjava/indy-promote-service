/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
package org.commonjava.service.promote.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.service.promote.client.repository.RepositoryService;
import org.commonjava.service.promote.client.storage.*;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.StoreKey;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_OK;
import static org.commonjava.indy.model.core.PathStyle.plain;

@ApplicationScoped
public class PromotionHelper
{
    public static final int DEFAULT_STORAGE_SERVICE_EXIST_CHECK_BATCH_SIZE = 1000;

    final static String PATH_STYLE_PROPERTY = "path_style";

    final static String TIMEOUT_SECONDS_PROPERTY = "cache_timeout_seconds";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static Predicate<String> isMetadataPredicate()
    {
        return compile( ".+/maven-metadata\\.xml(\\.(md5|sha[0-9]+))?" ).asPredicate();
    }

    public final static Predicate<String> CHECKSUM_PREDICATE = isChecksumPredicate();

    public static Predicate<String> isChecksumPredicate() {
        return compile( ".+\\.(md5|sha[0-9]+)" ).asPredicate();
    }

    @Inject
    @RestClient
    StorageService storageService;

    @Inject
    @RestClient
    RepositoryService repositoryService;

    public PromotionHelper()
    {
    }

    public void purgeSourceQuietly( StoreKey store, Set<String> paths )
    {
        delete( store, paths );
    }

    /**
     * Delete paths from target store.
     * @return Error messages.
     */
    public Set<String> delete(StoreKey store, Set<String> paths ) {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setFilesystem( store.toString() );
        request.setPaths( paths );
        Response resp = storageService.delete(request);

        Set<String> result = new HashSet<>();
        if ( Response.Status.fromStatusCode( resp.getStatus() ).getFamily()
                != Response.Status.Family.SUCCESSFUL  )
        {
            result.add( "Delete failed, resp status: " + resp.getStatus() );
        }

        BatchDeleteResult deleteResult = resp.readEntity(BatchDeleteResult.class);
        return deleteResult.getFailed();
    }

    static class RepoRetrievalResult
    {
        final List<String> errors;
        final StoreInfo targetStore, sourceStore;

        final PathStyle pathStyle;

        public RepoRetrievalResult(List<String> errors, StoreInfo sourceStore, StoreInfo targetStore )
        {
            this.errors = errors;
            this.targetStore = targetStore;
            this.sourceStore = sourceStore;
            this.pathStyle = sourceStore.pathStyle;
        }

        public boolean hasErrors()
        {
            return !errors.isEmpty();
        }
    }

    static class StoreInfo
    {
        final StoreKey storeKey;
        final PathStyle pathStyle;
        final int timeoutSeconds;

        StoreInfo(final StoreKey storeKey, final PathStyle pathStyle, int timeoutSeconds)
        {
            this.storeKey = storeKey;
            this.pathStyle = pathStyle;
            this.timeoutSeconds = timeoutSeconds;
        }

        @Override
        public String toString()
        {
            return "StoreInfo{" +
                    "storeKey=" + storeKey +
                    ", pathStyle=" + pathStyle +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }

    /**
     * Check whether the source and target repo exists and return useful information or errors.
     * @param request
     */
    RepoRetrievalResult retrieveSourceAndTargetRepos( PathsPromoteRequest request )
    {
        List<String> errors = new ArrayList<>();
        StoreInfo sourceStoreInfo = null;
        StoreInfo targetStoreInfo = null;

        try
        {
            sourceStoreInfo = getStoreInfo(request.getSource());
        }
        catch ( Exception e )
        {
            String msg = String.format( "Failed to retrieve source store: %s. Reason: %s", request.getSource(),
                                        e.getMessage() );
            logger.error( msg, e );
            errors.add( msg );
        }

        try
        {
            targetStoreInfo = getStoreInfo(request.getTarget());
        }
        catch ( Exception e )
        {
            String msg = String.format( "Failed to retrieve target store: %s. Reason: %s", request.getTarget(),
                                        e.getMessage() );
            logger.error( msg, e );
            errors.add( msg );
        }

        if ( sourceStoreInfo == null || targetStoreInfo == null )
        {
            errors.add(String.format("Failed to get source or target store info, source: %s, target: %s",
                    sourceStoreInfo, targetStoreInfo));
            return new RepoRetrievalResult( errors, null, null );
        }

        if ( !Objects.equals( sourceStoreInfo.pathStyle, targetStoreInfo.pathStyle) )
        {
            errors.add(String.format("Source and target store has different pathStyle, source: %s, target: %s",
                    sourceStoreInfo, targetStoreInfo));
            return new RepoRetrievalResult( errors, null, null );
        }

        return new RepoRetrievalResult( errors, sourceStoreInfo, targetStoreInfo );
    }

    private StoreInfo getStoreInfo(StoreKey storeKey)
    {
        StoreInfo ret = null;
        String pathStyle = null;
        int timeoutSeconds = 0;
        Response resp = repositoryService.getStore(storeKey.getPackageType(), storeKey.getType().getName(), storeKey.getName());
        if ( resp.getStatus() == SC_OK )
        {
            String content = resp.readEntity(String.class);
            logger.trace("Get repo definition, {}", content);
            try
            {
                Map<String, Object> map = getObjectMapFromJson(content);
                pathStyle = (String) map.get( PATH_STYLE_PROPERTY );
                Object timeout = map.get( TIMEOUT_SECONDS_PROPERTY );
                if ( timeout != null )
                {
                    timeoutSeconds = (Integer) timeout;
                }
            }
            catch (JsonProcessingException e)
            {
                logger.error("Failed to parse repo content", e);
                return null;
            }

            if ( isBlank(pathStyle) )
            {
                ret = new StoreInfo(storeKey, plain, timeoutSeconds);
            }
            else
            {
                ret = new StoreInfo(storeKey, PathStyle.valueOf( pathStyle ), timeoutSeconds);
            }
        }
        logger.info( "Get store info, store: {}, status code: {}, pathStyle: {}", storeKey, resp.getStatus(), pathStyle );
        return ret;
    }

    Map<String, Object> getObjectMapFromJson(String json) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map>(){});
    }

    public static long timeInSeconds( long begin )
    {
        return TimeUnit.MILLISECONDS.toSeconds( System.currentTimeMillis() - begin );
    }

    public static long timeInMillSeconds( long begin )
    {
        return System.currentTimeMillis() - begin;
    }
}
