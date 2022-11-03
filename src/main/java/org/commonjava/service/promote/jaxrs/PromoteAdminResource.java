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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.ValidationRuleDTO;
import org.commonjava.service.promote.model.ValidationRuleSet;
import org.commonjava.service.promote.util.ResponseHelper;
import org.commonjava.service.promote.validate.PromoteValidationsManager;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.function.Supplier;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag( name = "Promote Administration", description = "Resource for managing configurations for promotion." )
@Path( PromoteAdminResource.PROMOTION_ADMIN_API )
@ApplicationScoped
public class PromoteAdminResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String PROMOTION_ADMIN_API = "/api/promotion/admin";

    @Inject
    PromoteValidationsManager validationsManager;

    @Inject
    ObjectMapper mapper;

    @Inject
    ResponseHelper responseHelper;

    @ApiOperation( "Get all rules' names" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "All promotion validation rules' definition" ),
                           @ApiResponse( code = 404, message = "No rules are defined" ) } )
    @Path( "/validation/rules/all" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getAllRules( final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Map<String, ValidationRuleDTO> rules = validationsManager.toDTO().getRules();
            if ( !rules.isEmpty() )
            {
                return Response.ok( new ArrayList<>( rules.keySet() ) ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).entity( Collections.emptyList() ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule by rule name" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule definition" ),
                           @ApiResponse( code = 404, message = "The rule doesn't exist" ) } )
    @Path( "/validation/rules/named/{name}" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getRule( final @PathParam( "name" ) String ruleName,
                             final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Optional<ValidationRuleDTO> dto = validationsManager.getNamedRuleAsDTO( ruleName );
            if ( dto.isPresent() )
            {
                return Response.ok( dto.get() ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by store key" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/all" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getAllRuleSets( final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            Map<String, ValidationRuleSet> ruleSets = validationsManager.toDTO().getRuleSets();
            if ( !ruleSets.isEmpty() )
            {
                return Response.ok( new ArrayList<>( ruleSets.keySet() ) ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).entity( Collections.emptyList() ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by store key" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/storekey/{storeKey}" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getRuleSetByStoreKey( final @PathParam( "storeKey" ) String storeKey,
                                          final @Context SecurityContext securityContext,
                                          final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            final StoreKey storekey;
            try
            {
                storekey = StoreKey.fromString( storeKey );
            }
            catch ( Exception e )
            {
                return responseHelper.formatBadRequestResponse( e.getMessage() );
            }
            ValidationRuleSet ruleSet = validationsManager.getRuleSetMatching( storekey );
            if ( ruleSet != null )
            {
                return Response.ok( ruleSet ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    @ApiOperation( "Get promotion rule-set by name" )
    @ApiResponses( { @ApiResponse( code = 200, response = Response.class,
                                   message = "The promotion validation rule-set definition" ),
                           @ApiResponse( code = 404, message = "The rule-set doesn't exist" ) } )
    @Path( "/validation/rulesets/named/{name}" )
    @GET
    @Produces( APPLICATION_JSON )
    public Response getRuleSetByName( final @PathParam( "name" ) String name,
                                      final @Context SecurityContext securityContext, final @Context UriInfo uriInfo )
    {
        return checkEnabledAnd( () -> {
            final StoreKey storekey;
            Optional<ValidationRuleSet> ruleSet = validationsManager.getNamedRuleSet( name );
            if ( ruleSet.isPresent() )
            {
                return Response.ok( ruleSet.get() ).build();
            }
            else
            {
                return Response.status( Response.Status.NOT_FOUND ).build();
            }
        } );
    }

    private Response checkEnabledAnd( Supplier<Response> responseSupplier )
    {
        if ( validationsManager.isEnabled() )
        {
            return responseSupplier.get();
        }
        else
        {
            return responseHelper.formatBadRequestResponse(
                    "Content promotion is disabled. Please check your indy configuration for more info." );
        }
    }

}
