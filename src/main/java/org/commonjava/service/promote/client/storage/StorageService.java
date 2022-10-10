package org.commonjava.service.promote.client.storage;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/api/storage")
@RegisterRestClient(configKey="storage-service-api")
public interface StorageService
{
    @DELETE
    @Path("content/{filesystem}/{path: (.*)}")
    Response delete(final @PathParam( "filesystem" ) String filesystem, final @PathParam( "path" ) String path);

    @POST
    @Path( "copy" )
    FileCopyResult copy( final FileCopyRequest request );
}
