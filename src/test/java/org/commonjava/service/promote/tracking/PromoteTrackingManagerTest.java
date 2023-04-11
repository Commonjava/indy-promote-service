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
import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.PromoteTrackingRecords;
import org.commonjava.service.promote.model.StoreKey;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class PromoteTrackingManagerTest
{
    @Inject
    PromoteTrackingManager promoteTrackingManager;

    @Inject
    IndyObjectMapper objectMapper;

    @BeforeAll
    public static void init() throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    @Test
    public void run() throws Exception
    {
        String trackingId = "testId";

        // Add record 1
        StoreKey source = StoreKey.fromString("maven:remote:central");
        StoreKey target = StoreKey.fromString("maven:hosted:shared-imports");
        Set<String> paths = new HashSet<>();
        String path1 = "a/b/c";
        paths.add(path1);
        PathsPromoteRequest request = new PathsPromoteRequest(source, target, paths);
        String id1 = request.getPromotionId();
        PathsPromoteResult result = new PathsPromoteResult( request, emptySet(), paths, emptySet(), null);
        promoteTrackingManager.addTrackingRecord(trackingId, result);

        // Add record 2
        source = StoreKey.fromString("maven:hosted:build-1");
        target = StoreKey.fromString("maven:hosted:test-builds");
        paths = new HashSet<>();
        String path2 = "foo/bar/1.0/bar-1.0.jar";
        paths.add(path2);
        request = new PathsPromoteRequest(source, target, paths);
        String id2 = request.getPromotionId();
        result = new PathsPromoteResult( request, emptySet(), paths, emptySet(), null);
        promoteTrackingManager.addTrackingRecord(trackingId, result);

        // Get records
        Optional<PromoteTrackingRecords> optional = promoteTrackingManager.getTrackingRecords(trackingId);
        PromoteTrackingRecords records = optional.orElse(null);
        //System.out.println(">>>\n" + objectMapper.writeValueAsString(records));
        assertThat( records, notNullValue() );
        assertEquals(records.getTrackingId(), trackingId);
        assertEquals(2, records.getResultMap().size());
        assertTrue( records.getResultMap().get(id1).getCompletedPaths().contains(path1) );
        assertTrue( records.getResultMap().get(id2).getCompletedPaths().contains(path2) );
    }
}
