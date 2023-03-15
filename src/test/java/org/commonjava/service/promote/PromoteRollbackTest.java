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

/**
 * GIVEN:
 * <ul>
 *     <li>Two hosted repos source and target, two paths are loaded to source repo</li>
 * </ul>
 * WHEN:
 * <ul>
 *     <li>Two paths in source are promoted to target</li>
 * </ul>
 * THEN:
 * <ol>
 *     <li>Promotion succeeds with paths being promoted to target</li>
 * </ol>
 * WHEN:
 * <ul>
 *     <li>Rollback by the promote result</li>
 * </ul>
 * THEN:
 * <ol>
 *     <li>Promotion rollback succeeds with paths being present in source and no files left in target</li>
 * </ol>
 */
@QuarkusTest
public class PromoteRollbackTest {
    @Inject
    TestHelper testHelper;

    protected StoreKey source = new StoreKey("maven", StoreType.hosted, "source-rbk");

    protected StoreKey target = new StoreKey("maven", StoreType.hosted, "target-rbk");

    private final String first = "/first/path";

    private final String second = "/second/path";

    @BeforeEach
    public void prepare() throws IOException
    {
        testHelper.deployContent(source, first, "This is a test");
        testHelper.deployContent(source, second, "This is the second test");
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteRequest request = new PathsPromoteRequest(source, target);
        promoteThenRollback(request, testHelper);

        // Paths being restored to source and no files left in target
        assertThat(testHelper.exists(target, first), equalTo(false));
        assertThat(testHelper.exists(target, second), equalTo(false));
        assertThat(testHelper.exists(source, first), equalTo(true));
        assertThat(testHelper.exists(source, second), equalTo(true));
    }

    static void promoteThenRollback(PathsPromoteRequest request, TestHelper testHelper) throws Exception
    {
        PathsPromoteResult result = testHelper.doPromote(request);

        Set<String> pending = result.getPendingPaths();
        assertThat(pending == null || pending.isEmpty(), equalTo(true));

        Set<String> completed = result.getCompletedPaths();
        assertThat(completed, notNullValue());
        assertThat(completed.size(), equalTo(2));
        assertThat(result.getError(), nullValue());

        // Rollback
        result = testHelper.doRollback(result);

        completed = result.getCompletedPaths();
        assertThat(completed == null || completed.isEmpty(), equalTo(true));

        pending = result.getPendingPaths();
        assertThat(pending, notNullValue());
        assertThat(pending.size(), equalTo(2));
        assertThat(result.getError(), nullValue());
    }

}
