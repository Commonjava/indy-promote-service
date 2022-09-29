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

import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.StoreKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;


@ApplicationScoped
public class PromotionHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public PromotionHelper()
    {
    }

    public void purgeSourceQuietly( StoreKey src, Set<String> paths )
    {
        deleteViaStorageService( paths, src );
    }

    public List<String> deleteViaStorageService(Set<String> completed, StoreKey target) {
        // TODO: delete completed paths from target to revert the promotion
        return emptyList();
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
        // TODO: get artifact store via micro service (mainly to check if exists)
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
