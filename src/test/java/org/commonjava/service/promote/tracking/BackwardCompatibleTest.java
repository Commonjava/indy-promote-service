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
package org.commonjava.service.promote.tracking;

import io.quarkus.test.junit.QuarkusTest;
import net.bytebuddy.utility.RandomString;
import org.commonjava.service.promote.TestHelper;
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

/**
 * The request Json does NOT contain the 'trackingId' to simulate the requests from old promote client.
 */
@QuarkusTest
public class BackwardCompatibleTest
{
    @Inject
    TestHelper testHelper;

    private final String sourceName = "source" + RandomString.make();

    private final String targetName = "target" + RandomString.make();

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, sourceName);

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, targetName);;

    private final String path = "/backward/compatible/test";

    @BeforeEach
    public void prepare() throws IOException
    {
        testHelper.deployContent(source, path, "This is a test" );
    }

    private final String requestJson = "{" +
            "\"async\":false," +
            "\"callback\":null," +
            "\"source\":{\"packageType\":\"maven\",\"type\":\"hosted\",\"name\":\"" + sourceName + "\"}," +
            "\"target\":{\"packageType\":\"maven\",\"type\":\"hosted\",\"name\":\"" + targetName + "\"}," +
            "\"purgeSource\":false," +
            "\"fireEvents\":false," +
            "\"failWhenExists\":false" +
            "}";

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = testHelper.doPromote( requestJson );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );

        assertThat( result.getError(), nullValue() );
        assertThat( testHelper.exists( target, path ), equalTo( true ) );
    }
}
