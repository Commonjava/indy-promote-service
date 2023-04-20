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
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.model.*;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PromoteTrackingTest
{
    @Inject
    TestHelper testHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "source_tracking" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "target_tracking" );;

    private final String path1 = "/tracking/test/path1";

    @BeforeAll
    public static void init() throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    @BeforeEach
    public void prepare() throws IOException
    {
        testHelper.deployContent(source, path1, "This is a test" );
    }

    @Test
    public void run() throws Exception
    {
        final String trackingId = "build-" + new Random().nextInt();

        // Dry run will not change anything
        testHelper.doPromote( new PathsPromoteRequest( source, target )
                .setTrackingId(trackingId).setDryRun(true) );
        PromoteTrackingRecords records = testHelper.getTrackingRecords( trackingId );
        assertThat( records == null, equalTo( true ) );

        // Nominal promotion
        PathsPromoteRequest request = new PathsPromoteRequest(source, target)
                .setTrackingId(trackingId);
        String promotionId = request.getPromotionId();
        testHelper.doPromote(request);

        // Get tracking records
        records = testHelper.getTrackingRecords( trackingId );
        assertNotNull( records );
        assertEquals(trackingId, records.getTrackingId());
        Map<String, PathsPromoteResult> resultMap = records.getResultMap();
        assertThat( resultMap.size(), equalTo( 1 ) );

        // Get result from promotion record
        PathsPromoteResult result = resultMap.get(promotionId);
        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );
        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( result.getError(), nullValue() );

        // Rollback the previous promotion
        testHelper.doRollback(result);

        // Get tracking records again
        records = testHelper.getTrackingRecords( trackingId );
        assertNull( records );
    }

}
