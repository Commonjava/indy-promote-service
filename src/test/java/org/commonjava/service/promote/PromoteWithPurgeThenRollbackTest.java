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
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.IOException;

import static org.commonjava.service.promote.PromoteRollbackTest.promoteThenRollback;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class PromoteWithPurgeThenRollbackTest
{
    @Inject
    TestHelper testHelper;

    protected StoreKey source = new StoreKey("maven", StoreType.hosted, "source-p-rbk");

    protected StoreKey target = new StoreKey("maven", StoreType.hosted, "target-p-rbk");

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
        PathsPromoteRequest request = new PathsPromoteRequest(source, target).setPurgeSource(true);
        promoteThenRollback(request, testHelper);

        // Paths being restored to source and no files left in target
        assertThat(testHelper.exists(target, first), equalTo(false));
        assertThat(testHelper.exists(target, second), equalTo(false));
        assertThat(testHelper.exists(source, first), equalTo(true));
        assertThat(testHelper.exists(source, second), equalTo(true));
    }
}
