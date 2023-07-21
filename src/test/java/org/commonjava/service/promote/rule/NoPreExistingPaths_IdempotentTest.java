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
package org.commonjava.service.promote.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@QuarkusTest
public class NoPreExistingPaths_IdempotentTest
{
    @Inject
    TestHelper ruleTestHelper;

    private ObjectMapper mapper = new ObjectMapper();

    private final String resourceDir = "no-pre-existing-paths/";

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-idem" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    String path = "org/foo/valid/1.1/valid-1.1.pom";

    @BeforeEach
    public void prepare() throws IOException
    {
        // Deploy same file to both the resource and target
        ruleTestHelper.deployResource( source, path, resourceDir + "valid.pom.xml");
        ruleTestHelper.deployResource( target, path, resourceDir + "valid.pom.xml" );
    }

    @Test
    @Disabled("Disabled because no-pre-exising-rule changed to not check checksum")
    public void run() throws Exception
    {
        // Promotion the same file (with the same checksum)
        PathsPromoteResult result = ruleTestHelper.doPromote(
                new PathsPromoteRequest( source, target, path ).setPurgeSource( true ) );

        assertEquals( result.getRequest().getSource(), source );
        assertEquals( result.getRequest().getTarget(), target );

        assertNull( result.getError() );

        Set<String> pending = result.getPendingPaths();
        assertTrue( pending == null || pending.isEmpty() );

        Set<String> skipped = result.getSkippedPaths();
        assertNotNull( skipped );
        assertEquals( 1, skipped.size() );
    }

}
