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

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 'no-pre-existing-paths' rule reports validation error if files already exist in target repo, but ignore & skip
 * files if they have same checksum.
 * In this case, we put same pom files but different jar files to source and target to check:
 *   A. the idempotent pom file DOES NOT trigger validation error
 *   B. different jar file triggers validation error
 */
@QuarkusTest
public class NoPreExistingPaths_IdempotentTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final String resourceDir = "no-pre-existing-paths/";

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-idem" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    String path = "/test/empty/1.0.0.redhat-00297/empty-1.0.0.redhat-00297.pom";

    String jarPath = "/test/empty/1.0.0.redhat-00297/empty-1.0.0.redhat-00297.jar";

    @BeforeEach
    public void prepare() throws IOException
    {
        // Put same pom files to both resource and target
        ruleTestHelper.deployResource( source, path, resourceDir + "valid.pom.xml");
        ruleTestHelper.deployResource( target, path, resourceDir + "valid.pom.xml" );

        // Put different jar files to resource and target to trigger checksum validation error
        ruleTestHelper.deployContent( source, jarPath, "This is 1.0 content");
        ruleTestHelper.deployContent( target, jarPath, "This is changed content" );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(
                new PathsPromoteRequest( source, target ).setPurgeSource( true ) );

        // different jar file triggers validation error
        assertEquals( jarPath + " is already available in maven:hosted:test-builds with different checksum",
                result.getValidations().getValidatorErrors().get("no-pre-existing-paths"));

        assertNotNull( result.getError() );

        Set<String> pending = result.getPendingPaths();
        assertFalse( pending.isEmpty() );

        Set<String> skipped = result.getSkippedPaths();
        assertTrue( skipped.isEmpty() );
    }

}
