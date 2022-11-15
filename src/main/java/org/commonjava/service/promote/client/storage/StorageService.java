package org.commonjava.service.promote.client.storage;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/storage")
@RegisterRestClient(configKey="storage-service-api")
public interface StorageService
{
    @DELETE
    @Path("content/{filesystem}/{path: (.*)}")
    Response delete(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @DELETE
    @Path("filesystem")
    Response delete(final BatchDeleteRequest request );

    @GET
    @Path("filesystem/exist")
    Response exist(final BatchExistRequest request );

    @GET
    @Path("content/{filesystem}/{path: (.*)}")
    Response retrieve(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @PUT
    @POST
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
