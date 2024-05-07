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
package org.commonjava.service.promote.client.content;

import org.commonjava.indy.service.security.jaxrs.CustomClientRequestFilter;
import org.commonjava.service.promote.client.ErrorResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/content")
@RegisterRestClient(configKey="content-service-api")
@RegisterProvider(CustomClientRequestFilter.class)
@RegisterProvider(ErrorResponseExceptionMapper.class)
public interface ContentService
{
    @GET
    @Path("{package}/{type: (hosted|group|remote)}/{name}/{path: (.*)}")
    Response retrieve(final @PathParam( "package" ) String packageName,
                      final @PathParam( "type") String type,
                      final @PathParam( "name") String name,
                      final @PathParam( "path" ) String path) throws Exception;

    @HEAD
    @Path("{package}/{type: (hosted|group|remote)}/{name}/{path: (.*)}")
    Response exists(final @PathParam( "package" ) String packageName,
                    final @PathParam( "type") String type,
                    final @PathParam( "name") String name,
                    final @PathParam( "path" ) String path) throws Exception;

}
