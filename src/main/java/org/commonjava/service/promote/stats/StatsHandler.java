package org.commonjava.service.promote.stats;

import org.commonjava.service.promote.util.ResponseHelper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path( "/api/stats" )
public class StatsHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    Versioning versioning;

    @Inject
    ResponseHelper responseHelper;

    @Operation( summary = "Retrieve versioning information about this APP instance" )
    @Path( "version-info" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getAppVersion()
    {
        return responseHelper.formatOkResponseWithJsonEntity( versioning );
    }

}
