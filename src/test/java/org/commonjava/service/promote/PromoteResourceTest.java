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
package org.commonjava.service.promote;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import static org.commonjava.service.promote.TestHelper.VALID_POM_EXAMPLE;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class PromoteResourceTest
{
    public static final String PROMOTE_PATH = "/api/promotion/paths/promote",
        ROLLBACK_PATH = "/api/promotion/paths/rollback";

    @Inject
    TestHelper testHelper;

    @Test
    public void testPromoteAndRollback() throws Exception {
        StoreKey src = new StoreKey( "maven", StoreType.hosted, "build-1" );
        StoreKey tgt = new StoreKey( "maven", StoreType.hosted, "test-builds" );
        Set<String> paths = new HashSet<>();
        String pathPom = "foo/bar/1.0/bar-1.0.pom";
        String pathJar = "foo/bar/1.0/bar-1.0.jar";
        paths.add( pathPom );
        paths.add( pathJar );
        PathsPromoteRequest promoteRequest = new PathsPromoteRequest( src, tgt, paths );

        // Prepare src file
        testHelper.deployContent(src, pathPom, VALID_POM_EXAMPLE);
        testHelper.deployContent(src, pathJar, "This is a jar even not looks like it...");

        // Promote
        PathsPromoteResult result = testHelper.doPromote( promoteRequest );

        assertNotNull( result );
        assertNull( result.getError() );
        assertTrue( result.getCompletedPaths().containsAll(paths) );
        assertTrue( result.getSkippedPaths().isEmpty() );
        assertTrue( result.getPendingPaths().isEmpty() );

        // Rollback
        result = testHelper.doRollback( result );

        assertNotNull( result );
        assertNull( result.getError() );
        assertTrue( result.getCompletedPaths().isEmpty() ); // rollback dumps the completed back to pending
        assertTrue( result.getSkippedPaths().isEmpty() );
        assertTrue( result.getPendingPaths().containsAll(paths) );
    }

}