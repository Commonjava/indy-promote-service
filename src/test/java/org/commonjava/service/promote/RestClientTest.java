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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.fixture.TestRestClient;
import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.util.ResponseHelper;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RestClient will throw WebApplicationException when it hit 404. But we need the status code
 * rather than an exception in case of existence check. This case verifies we can get the right status via
 * responseHelper.isRest404Exception(e).
 */
@QuarkusTest
@QuarkusTestResource( TestResources.class )
public class RestClientTest {
    @Inject
    @Any
    TestRestClient errorRestClient;

    @Inject
    ResponseHelper responseHelper;

    @Test
    public void run() throws Exception
    {
        // Normal retrieve
        assertTrue(errorRestClient.get200Resource() != null);

        // 404 response
        try
        {
            errorRestClient.get404Resource();
        }
        catch (Exception e)
        {
            if (responseHelper.isRest404Exception(e))
            {
                // Get 404 which is expected
                return;
            }
            throw e; // fail
        }
    }
}
