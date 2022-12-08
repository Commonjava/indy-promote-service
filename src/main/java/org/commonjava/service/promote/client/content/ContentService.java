package org.commonjava.service.promote.client.content;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/api/content")
@RegisterRestClient(configKey="content-service-api")
public interface ContentService
{
    @GET
    @Path("{package}/{type: (hosted|group|remote)}/{name}/{path: (.*)}")
    Response retrieve(final @PathParam( "package" ) String packageName,
                      final @PathParam( "type") String type,
                      final @PathParam( "name") String name,
                      final @PathParam( "path" ) String path);

    @HEAD
    @Path("{package}/{type: (hosted|group|remote)}/{name}/{path: (.*)}")
    Response exists(final @PathParam( "package" ) String packageName,
                    final @PathParam( "type") String type,
                    final @PathParam( "name") String name,
                    final @PathParam( "path" ) String path);

}
