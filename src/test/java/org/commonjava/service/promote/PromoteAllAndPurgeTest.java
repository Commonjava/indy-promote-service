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
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class PromoteAllAndPurgeTest
{
    @Inject
    TestHelper testHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "source" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "target" );;

    private final String first = "/first/path";

    private final String second = "/second/path";

    @BeforeEach
    public void prepare() throws IOException
    {
        testHelper.deployContent(source, first, "This is a test" );
        testHelper.deployContent(source, second, "This is the second test" );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = testHelper.doPromote( new PathsPromoteRequest( source, target ) );
        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );
        assertThat( testHelper.exists( target, first ), equalTo( true ) );
        assertThat( testHelper.exists( target, second ), equalTo( true ) );

        // Purge test
        result = testHelper.doPromote( new PathsPromoteRequest( source, target ).setPurgeSource(true) );
        assertThat( testHelper.exists( source, first ), equalTo( false ) );
        assertThat( testHelper.exists( source, second ), equalTo( false ) );
    }
}
