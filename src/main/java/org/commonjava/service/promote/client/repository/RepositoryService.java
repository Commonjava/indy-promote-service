package org.commonjava.service.promote.client.repository;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/api/admin/stores")
@RegisterRestClient(configKey="repo-service-api")
public interface RepositoryService
{
    @GET
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response getStore(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name);

/*
    @PUT
    @Path("/{packageType}/{type: (hosted|group|remote)}/{name}")
    Response putStore(@PathParam("packageType") String packageType, @PathParam("type") String type, @PathParam("name") String name);

    @POST
    @Path("/{packageType}/{type: (hosted|group|remote)}")
    Response create(@PathParam( "packageType" ) String packageType, @PathParam( "type" ) String type);
*/

/*
    @GET
    @Path( "/query/affectedBy" )
    Response getGroupsAffectedBy( final @QueryParam( "keys" ) String keys );
*/

}
