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

import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.disjoint;

/**
 * This class is used to check whether the are more than one promotion requests promoting some paths to same target repo.
 *
 * TODO: conflict manager will not work as expected when moved to promotion service, as different nodes cannot sync
 * with each other. We may need a third party service to hold the 'inUseMap'. This again needs a locking mechanism that
 * were designed for a cluster environment.
 *
 * On a second look, this conflict manager serves the 'failWhenExists'. Actually this flag is not meant to check concurrent
 * promotions. Rather, it is to check conflict paths in target repo (e.g, 'pnc-builds' needs the flag to be ture,
 * while 'shared-imports' does not care. This kind of conflict checks are carried over by validation rules.)
 * Given this understanding, checking the in-memory concurrent promotion conflicts has little meaning at all.
 *
 * Probably we can just abandon this class in future. At this moment, I just keep it as is.
 * ruhan Sep 22, 2022.
 */
@ApplicationScoped
public class PathConflictManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<StoreKey, Set<StoreKeyPaths>> inUseMap = new HashMap<>();

    public PathsPromoteResult checkAnd(StoreKeyPaths k, Function<StoreKeyPaths, PathsPromoteResult> function,
                                       Function<StoreKeyPaths, PathsPromoteResult> conflictedFunction )
    {
        Set<StoreKeyPaths> inUse = null;
        boolean conflicted = false;
        try
        {
            logger.debug( "Check paths conflict for {}", k );
            synchronized ( inUseMap )
            {
                inUse = inUseMap.get( k.getTarget() );
                if ( inUse == null )
                {
                    inUse = new HashSet<>();
                    inUse.add( k );
                    inUseMap.put( k.getTarget(), inUse );
                }
                else
                {
                    conflicted = hasConflict( k.getPaths(), inUse );
                    if ( !conflicted )
                    {
                        inUse.add( k );
                    }
                }
            }

            logger.debug( "Check done, conflicted: {}", conflicted );
            if ( conflicted )
            {
                return conflictedFunction.apply( k );
            }
            else
            {
                return function.apply( k );
            }
        }
        finally
        {
            // clean up
            synchronized ( inUseMap )
            {
                inUse.remove( k );
                if ( inUse.isEmpty() )
                {
                    inUseMap.remove( k.getTarget() );
                }
            }
        }
    }

    private boolean hasConflict( Set<String> paths, Set<StoreKeyPaths> inUse )
    {
        for ( StoreKeyPaths keyPaths : inUse )
        {
            Set<String> s = keyPaths.getPaths();
            if ( !disjoint( paths, s ) )
            {
                logger.warn( "Conflict detected, key: {}, paths: {}, inUse: {}", keyPaths.getTarget(), paths, s );
                return true;
            }
        }
        return false;
    }
}
