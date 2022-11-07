package org.commonjava.service.promote.client.content;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/api/content/{package}/{type: (hosted|group|remote)}/{name}/{path: (.*)}")
@RegisterRestClient(configKey="content-service-api")
public interface ContentService
{
    @GET
    Response retrieve(final @PathParam( "package" ) String packageName,
                      final @PathParam( "type") String type,
                      final @PathParam( "name") String name,
                      final @PathParam( "path" ) String path);

    @HEAD
    Response exists(final @PathParam( "package" ) String packageName,
                    final @PathParam( "type") String type,
                    final @PathParam( "name") String name,
                    final @PathParam( "path" ) String path);

}
