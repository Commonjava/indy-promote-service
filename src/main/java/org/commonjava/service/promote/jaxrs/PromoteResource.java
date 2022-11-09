/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.service.promote.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.io.IOUtils;

import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.exception.PromotionException;
import org.commonjava.service.promote.core.PromotionManager;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.util.ResponseHelper;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.commonjava.service.promote.util.JaxRsUriFormatter.getBaseUrlByStoreKey;

@Tag( name = "Content Promotion", description = "Promote content from a source repository to a target repository." )
@Path( "/api/promotion" )
@Produces( APPLICATION_JSON )
@ApplicationScoped
public class PromoteResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    PromotionManager manager;

    @Inject
    IndyObjectMapper mapper;

    @Inject
    ResponseHelper responseHelper;

    @ApiOperation( "Promote paths from a source repository into a target repository/group (subject to validation)." )
    @ApiResponse( code=200, message = "Promotion operation finished (consult response content for success/failure).", response= PathsPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON request specifying source and target, with other configuration options",
                       required = true )
    @Path( "/paths/promote" )
    @POST
    @Consumes( APPLICATION_JSON )
    public Response promotePaths( final @Context HttpRequest request, final @Context UriInfo uriInfo )
    {
        PathsPromoteRequest req;
        Response response;
        try
        {
            final String json = IOUtils.toString( request.getInputStream(), defaultCharset() );
            logger.info( "Got promotion request:\n{}", json );
            req = mapper.readValue( json, PathsPromoteRequest.class );
        }
        catch ( final IOException e )
        {
            response = responseHelper.formatResponse( e, "Failed to read DTO from request body." );
            return response;
        }

        try
        {
            final String baseUrl = getBaseUrlByStoreKey( uriInfo, req.getSource() );
            final PathsPromoteResult result = manager.promotePaths( req, baseUrl );

            response = responseHelper.formatOkResponseWithJsonEntity( result );
            logger.info( "Send promotion result:\n{}", response.getEntity() );
        }
        catch ( PromotionException e )
        {
            logger.error( e.getMessage(), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Rollback promotion of any completed paths to a source repository from a target repository/group." )
    @ApiResponse( code=200, message = "Promotion operation finished (consult response content for success/failure).", response=PathsPromoteResult.class )
    @ApiImplicitParam( name = "body", paramType = "body",
                       value = "JSON result from previous attempt, specifying source and target, with other configuration options",
                       allowMultiple = false, required = true,
                       dataType = "PathsPromoteResult" )
    @Path( "/paths/rollback" )
    @POST
    @Consumes( APPLICATION_JSON )
    public Response rollbackPaths(final @Context HttpRequest request, @Context final UriInfo uriInfo  )
    {
        PathsPromoteResult result;
        Response response;
        try
        {
            result = mapper.readValue( request.getInputStream(), PathsPromoteResult.class );
        }
        catch ( final IOException e )
        {
            response = responseHelper.formatResponse( e, "Failed to read DTO from request body." );
            return response;
        }

        try
        {
            result = manager.rollbackPathsPromote( result );
            response = responseHelper.formatOkResponseWithJsonEntity( result );
        }
        catch ( PromotionException e )
        {
            logger.error( e.getMessage(), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
