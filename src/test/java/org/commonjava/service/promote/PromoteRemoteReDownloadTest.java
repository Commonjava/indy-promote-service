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
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * GIVEN:
 * <ul>
 *     <li>RemoteRepository A with path P but never retrieved (same to retrieved & expired, to simplify the test)</li>
 *     <li>HostedRepository B</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>RemoteRepository A with path P is promoted to B</li>
 * </ul>
 * <br/>
 * THEN:
 * <ol>
 *     <li>Promotion succeeds with path P being copied to B</li>
 * </ol>
 */
@QuarkusTest
public class PromoteRemoteReDownloadTest
{
    @Inject
    TestHelper testHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.remote, "source" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "target" );;

    private static final String CONTENT = "This is a test";

    private static final String REMOTE = "remote";

    public static final String PATH_MISSING_BUT_CAN_BE_RE_DOWNLOAD = "/path/to/something/missing/but/can/be/re/downloaded";

    @BeforeEach
    public void prepare() throws Exception
    {
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = testHelper.doPromote( new PathsPromoteRequest( source, target,
                PATH_MISSING_BUT_CAN_BE_RE_DOWNLOAD ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );
        assertThat( result.getError(), nullValue() );
    }
}
