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
package org.commonjava.service.promote.client.storage;

import org.commonjava.service.promote.client.CustomClientRequestFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

@Path("/api/storage")
@RegisterRestClient(configKey="storage-service-api")
@RegisterProvider(CustomClientRequestFilter.class)
public interface StorageService
{
    @DELETE
    @Path("content/{filesystem}/{path: (.*)}")
    Response delete(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @POST
    @Path("filesystem")
    Response delete(final BatchDeleteRequest request );

    @POST
    @Path("filesystem/exist")
    Response exist(final BatchExistRequest request );

    @GET
    @Path("content/{filesystem}/{path: (.*)}")
    Response retrieve(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @PUT
    @Path( "content/{filesystem}/{path: (.*)}" )
    Response put(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path, final InputStream in);

    @POST
    @Path( "copy" )
    Response copy( final FileCopyRequest request );

    @HEAD
    @Path("content/{filesystem}/{path: (.*)}")
    Response exists(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @GET
    @Path( "browse{path: (.*)}" )
    Response list(final @PathParam( "path" ) String rawPath,
                      final @QueryParam( "recursive" ) boolean recursive,
                      final @QueryParam( "filetype" ) String fileType,
                      final @QueryParam( "limit" ) int limit );
}
