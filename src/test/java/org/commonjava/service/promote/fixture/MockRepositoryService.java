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
package org.commonjava.service.promote.fixture;

import io.quarkus.test.Mock;
import org.commonjava.service.promote.client.repository.RepositoryService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.core.Response;

import static org.apache.http.HttpStatus.SC_OK;

@Mock
@RestClient
public class MockRepositoryService implements RepositoryService
{
    @Override
    public Response getStore(String packageType, String type, String name)
    {
        String content;
        if ( name.startsWith("generic") )
        {
            content = "{\"path_style\": \"hashed\"}"; // Use hashed path_style for generic* repos
        }
        else
        {
            content = "{\"path_style\": \"plain\"}";
        }
        return Response.status( SC_OK ).entity(content).build();
    }
}
