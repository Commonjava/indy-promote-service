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
package org.commonjava.service.promote;

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Check that metadata should be skipped correctly during promotion with no error.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepository target contains paths for http metadata and signature metadata for a maven metadata.</li>
 *     <li>HostedRepository target set to readonly</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Repository source will promote same paths to target</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>These meta files can be promoted to target of skipping with no error</li>
 * </ul>
 */
@QuarkusTest
public class PromoteSkipMetadataTest
{
    @Inject
    TestHelper testHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "source" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "target" );;

    private static final String META_PATH = "/org/foo/bar/maven-metadata.xml",
            HTTP_META_PATH = "/org/foo/bar/maven-metadata.xml.http-metadata.json",
            MD5_META_PATH = "/org/foo/bar/maven-metadata.xml.md5";

    @BeforeEach
    public void prepare() throws IOException
    {
        testHelper.deployContent(source, META_PATH, "This is a metadata." );
        testHelper.deployContent(source, MD5_META_PATH, "md5 in source" );
        testHelper.deployContent(source, HTTP_META_PATH, "{\"test\":\"source\"}" );

        testHelper.deployContent(target, HTTP_META_PATH, "{\"test\":\"target\"}" );
        testHelper.deployContent(target, MD5_META_PATH, "md5 in target" );
    }

    @Test
    public void run()
            throws Exception
    {
        PathsPromoteRequest request =
                new PathsPromoteRequest( source, target,
                        META_PATH, HTTP_META_PATH, MD5_META_PATH );

        request.setFailWhenExists( true );

        PathsPromoteResult response = testHelper.doPromote( request );

        assertThat( response.succeeded(), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains( HTTP_META_PATH ), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains( MD5_META_PATH ), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains(META_PATH), equalTo( true ) );
    }

}
