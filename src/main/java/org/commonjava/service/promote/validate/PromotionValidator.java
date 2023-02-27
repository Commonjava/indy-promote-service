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
package org.commonjava.service.promote.validate;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;

import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.core.ContentDigester;
import org.commonjava.service.promote.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.join;

import static org.commonjava.service.promote.util.PoolUtils.detectOverloadVoid;

@ApplicationScoped
public class PromotionValidator
{
    private static final String PROMOTE_REPO_PREFIX = "Promote_";

    private static final String PROMOTION_VALIDATION_RULE = "promotion-validation-rule";

    private static final String PROMOTION_VALIDATION_RULE_SET = "promotion-validation-rule-set";

    @Inject
    PromoteValidationsManager validationsManager;

    @Inject
    PromotionValidationTools validationTools;

/*
    @Inject
    @RestClient
    RepositoryService repositoryService;
*/

    @Inject
    PromoteConfig config;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "promote-rules-runner", threads = 16, priority = 5,
            loadSensitive = ExecutorConfig.BooleanLiteral.TRUE, maxLoadFactor = 400 )
    WeftExecutorService validateService;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected PromotionValidator()
    {
    }

    public ValidationResult validate(PromoteRequest request, String baseUrl )
            throws PromotionValidationException
    {
        ValidationResult result = new ValidationResult();

        ValidationRuleSet set = validationsManager.getRuleSetMatching( request.getTarget() );

/*
        ArtifactStore source;
        try
        {
            source = getStore( request.getSource() );
        }
        catch ( Exception e )
        {
            throw new PromotionValidationException(
                    String.format( "Failed to retrieve source store: %s for validation", request.getSource() ), e );
        }
*/

        if ( set != null )
        {
            result.setRuleSet( set.getName() );
            //RequestContextHelper.setContext( PROMOTION_VALIDATION_RULE_SET, set.getName() );

            List<String> ruleNames = set.getRuleNames();
            logger.debug( "Running promotion validation rule-set: {}, rules: {}", set.getName(), ruleNames );
            if ( ruleNames != null && !ruleNames.isEmpty() )
            {
//                final ArtifactStore store = getRequestStore( request, baseUrl );
                final ValidationRequest validationRequest = new ValidationRequest( request, set, validationTools );
                try
                {
                    DrainingExecutorCompletionService<Exception> svc =
                            new DrainingExecutorCompletionService<>( validateService );

                    detectOverloadVoid(()->{
                        for ( String ruleRef : ruleNames )
                        {
                            svc.submit( () -> {
                                //RequestContextHelper.setContext( PROMOTION_VALIDATION_RULE, ruleRef );
                                Exception err = null;
                                try
                                {
                                    executeValidationRule( ruleRef, validationRequest, result, request );
                                }
                                catch ( Exception e )
                                {
                                    err = e;
                                }
                                finally
                                {
                                    //RequestContextHelper.clearContext( PROMOTION_VALIDATION_RULE );
                                }

                                return err;
                            } );
                        }
                    });

                    List<String> errors = new ArrayList<>();
                    svc.drain( err->{
                        if ( err != null )
                        {
                            logger.error( "Promotion validation failure", err );
                            errors.add( err.getMessage() );
                        }
                    } );

                    if ( !errors.isEmpty() )
                    {
                        throw new PromotionValidationException( format( "Failed to do promotion validation: \n\n%s", join( errors, "\n" ) ) );
                    }
                }
                catch ( InterruptedException | ExecutionException e )
                {
                    throw new PromotionValidationException( "Failed to execute promotion validations", e );
                }
/*
                finally
                {
                    if ( needTempRepo( request ) )
                    {
                        try
                        {
                            final String changeSum = format( "Removes the temp remote repo [%s] after promote operation.", store );
                            storeDataMgr.deleteArtifactStore( store.getKey(),
                                    new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                            changeSum ),
                                    new EventMetadata().set( ContentManager.SUPPRESS_EVENTS,
                                            true ) );

                            Transfer root = downloadManager.getStoreRootDirectory( store );
                            if ( root.exists() )
                            {
                                root.delete( false );
                            }

                            logger.debug( "Promotion temporary repo {} has been deleted for {}", store.getKey(),
                                    request.getSource() );
                        }
                        catch ( Exception e )
                        {
                            logger.warn( "Temporary promotion validation repository was NOT removed correctly.", e );
                        }
                    }
                }
*/
                return result;
            }
            else
            {
                logger.info( "No validation rules are defined for: {}", request.getTarget() );
                return result;
            }
        }
        else
        {
            logger.info( "No validation rule-sets are defined for: {}", request.getTarget() );
            return result;
        }
    }

    private void executeValidationRule( final String ruleRef, final ValidationRequest validationRequest,
                                        final ValidationResult result, final PromoteRequest request )
            throws PromotionValidationException
    {
        String ruleName = validationsManager.normalizeRuleName( new File( ruleRef ).getName() );
        ValidationRuleMapping rule = validationsManager.getRuleMappingNamed( ruleName );
        if ( rule != null )
        {
            logger.debug( "Running promotion validation rule: {}", rule.getName() );
            String error = null;
            {
                try
                {
                    error = rule.getRule().validate( validationRequest );
                }
                catch ( Exception e )
                {
                    throwValidationException( e, rule, request );
                }
            }

            if ( isNotEmpty( error ) )
            {
                logger.debug( "{} failed with error: {}", rule.getName(), error );
                result.addValidatorError( rule.getName(), error );
            }
            else
            {
                logger.debug( "{} succeeded", rule.getName() );
            }
        }
    }

    private void throwValidationException( Exception e, ValidationRuleMapping rule, PromoteRequest request )
            throws PromotionValidationException
    {
        if ( e instanceof PromotionValidationException )
        {
            throw (PromotionValidationException) e;
        }
        throw new PromotionValidationException(
                String.format( "Failed to run validation rule: %s, Reason: %s", rule.getName(), e.getMessage()),
                e, rule.getName() );
    }

/*
    public ArtifactStore getStore(StoreKey key )
    {
        Response response;
        try
        {
            response = repositoryService.getStore(key.getPackageType(), key.getType().name(), key.getName());
        }
        catch ( WebApplicationException e )
        {
            if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND )
            {
                return null;

            }
            else
            {
                throw e;
            }
        }
        if ( response != null && response.getStatus() == HttpStatus.SC_OK )
        {
            return response.readEntity(ArtifactStore.class);
        }
        return null;
    }
*/

/*
 * Why creating temp remote repo is needed? Before validation, a temp remote repo is created and points to the
 * promote source repo. This was used for performance or solving some sort of race condition. Yet no one really know
 * whether this is still needed. Hereby I comment out the temp repo code. ruhan Oct 2022.
 *
    private boolean needTempRepo( PromoteRequest promoteRequest )
            throws PromotionValidationException
    {
        if ( promoteRequest instanceof PathsPromoteRequest)
        {
            final Set<String> reqPaths = ( (PathsPromoteRequest) promoteRequest ).getPaths();
            return reqPaths != null && !reqPaths.isEmpty();
        }
        else
        {
            throw new PromotionValidationException( "The promote request is not a valid request, should not happen" );
        }
    }

    private ArtifactStore getRequestStore( PromoteRequest promoteRequest, String baseUrl )
            throws PromotionValidationException
    {
        final ArtifactStore store;
        final Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Promotion baseUrl: {} ", baseUrl );

        if ( needTempRepo( promoteRequest ) )
        {
            logger.info( "Promotion temporary repo is needed for {}, points to {} ", promoteRequest.getSource(),
                    baseUrl );
            final PathsPromoteRequest pathsReq = (PathsPromoteRequest) promoteRequest;

            String tempName =
                    PROMOTE_REPO_PREFIX + "tmp_" + pathsReq.getSource().getName() + "_" + new SimpleDateFormat(
                            "yyyyMMdd.hhmmss.SSSZ" ).format( new Date() );

            String packageType = pathsReq.getSource().getPackageType();

            final RemoteRepository tempRemote = new RemoteRepository( packageType, tempName, baseUrl );

            tempRemote.setPathMaskPatterns( pathsReq.getPaths() );

            store = tempRemote;
            try
            {
                final ChangeSummary changeSummary =
                        new ChangeSummary( ChangeSummary.SYSTEM_USER, "create temp remote repository" );

                storeDataMgr.storeArtifactStore( tempRemote, changeSummary, false, true, new EventMetadata() );
            }
            catch ( Exception e )
            {
                throw new PromotionValidationException( "Can not store the temp remote repository correctly", e );
            }
        }
        else
        {
            logger.info( "Promotion temporary repo is not needed for {} ", promoteRequest.getSource() );
            try
            {
                store = storeDataMgr.getArtifactStore( promoteRequest.getSource() );
            }
            catch ( Exception e )
            {
                throw new PromotionValidationException( "Failed to retrieve source ArtifactStore: {}. Reason: {}", e,
                        promoteRequest.getSource(), e.getMessage() );
            }
        }
        return store;
    }
*/
}
