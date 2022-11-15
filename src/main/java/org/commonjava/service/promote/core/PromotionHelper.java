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
package org.commonjava.service.promote.core;

import org.commonjava.service.promote.client.storage.*;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.StoreKey;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.regex.Pattern.compile;

@ApplicationScoped
public class PromotionHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static Predicate<String> isMetadataPredicate()
    {
        return compile( ".+/maven-metadata\\.xml(\\.(md5|sha[0-9]+))?" ).asPredicate();
    }

    public static Predicate<String> isChecksumPredicate() {
        return compile( ".+\\.(md5|sha[0-9]+)" ).asPredicate();
    }

    @Inject
    @RestClient
    private StorageService storageService;

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

    static class PromotionRepoRetrievalResult
    {
        final List<String> errors;
        final StoreKey targetStore, sourceStore;

        public PromotionRepoRetrievalResult( List<String> errors, StoreKey sourceStore, StoreKey targetStore )
        {
            this.errors = errors;
            this.targetStore = targetStore;
            this.sourceStore = sourceStore;
        }

        public boolean hasErrors()
        {
            return !errors.isEmpty();
        }
    }

    /**
     * Check whether the source and target repo exists.
     * @param request
     * @return errors
     */
    PromotionRepoRetrievalResult checkAndRetrieveSourceAndTargetRepos( PathsPromoteRequest request )
    {
        List<String> errors = new ArrayList<>();
        StoreKey sourceStore = null;
        StoreKey targetStore = null;

        try
        {
            sourceStore = getArtifactStoreViaService( request.getSource() );
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
            targetStore = getArtifactStoreViaService( request.getTarget() );
        }
        catch ( Exception e )
        {
            String msg = String.format( "Failed to retrieve target store: %s. Reason: %s", request.getTarget(),
                                        e.getMessage() );
            logger.error( msg, e );
            errors.add( msg );
        }

        return new PromotionRepoRetrievalResult( errors, sourceStore, targetStore );
    }

    private StoreKey getArtifactStoreViaService(StoreKey storeKey) {
        // TODO: get artifact store via micro service (mainly to check if exists). Can we skip this?
        return storeKey;
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
