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
package org.commonjava.service.promote.core;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.cdi.util.weft.*;

import org.commonjava.event.promote.PathsPromoteCompleteEvent;
import org.commonjava.service.promote.callback.PromotionCallbackHelper;
import org.commonjava.service.promote.client.content.ContentService;
import org.commonjava.service.promote.client.kafka.KafkaEventDispatcher;
import org.commonjava.service.promote.client.storage.*;
import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.exception.PromotionException;
import org.commonjava.service.promote.model.*;

import org.commonjava.service.promote.validate.PromotionValidator;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.*;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import static org.commonjava.service.promote.core.PromotionHelper.*;
import static org.commonjava.service.promote.util.PoolUtils.detectOverload;

/**
 * Component responsible for orchestrating the transfer of artifacts from one store to another
 * according to the given {@link PathsPromoteRequest} or {@link PathsPromoteResult}.
 */
@ApplicationScoped
public class PromotionManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    PromoteConfig config;

    @Inject
    PromotionValidator validator;

    @Inject
    PathConflictManager conflictManager;

    @WeftManaged
    @Inject
    @ExecutorConfig( named = "promotion", threads = 8, priority = 8, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    WeftExecutorService promotionService;

    @Inject
    PromotionCallbackHelper callbackHelper;

    @Inject
    PromotionHelper promotionHelper;

    @Inject
    KafkaEventDispatcher kafkaEventDispatcher;

    @Inject
    @RestClient
    private StorageService storageService;

    @Inject
    @RestClient
    private ContentService contentService;

    private static String TYPE_FILE = "file"; // for listing

    protected PromotionManager()
    {
    }

    public PromotionManager( PromotionValidator validator,
                             PromoteConfig config, WeftExecutorService promotionService )
    {
        this.validator = validator;
        this.config = config;
        this.promotionService = promotionService;
        this.promotionHelper = new PromotionHelper();
        this.conflictManager = new PathConflictManager();
    }

    /**
     * Promote artifacts from the source to the target given the {@link PathsPromoteRequest}. If paths are given, promote them.
     * Otherwise, build a recursive list of available artifacts in the source store and promote them.
     *
     * @param request containing source and target store keys, and an optional list of paths
     * @return The result including the source and target store keys, the paths completed (promoted successfully),
     * or errors explaining what (if anything) went wrong.
     *
     * IMPORTANT: Since 1.8, we use all-or-nothing policy, i.e., if anything fails we revert previous promoted paths.
     */
    public PathsPromoteResult promotePaths( final PathsPromoteRequest request, final String baseUrl )
            throws PromotionException
    {
/*
 * TODO: Ignore MDC and Span related stuff for now. We will come back and rearrange that for all micro services.
 *
        RequestContextHelper.setContext( PROMOTION_ID, request.getPromotionId() );
        RequestContextHelper.setContext( PROMOTION_TYPE, PATH_PROMOTION );
        RequestContextHelper.setContext( PROMOTION_SOURCE, request.getSource().toString() );
        RequestContextHelper.setContext( PROMOTION_TARGET, request.getTargetKey().toString() );
*/
        Future<PathsPromoteResult> future = submitPathsPromoteRequest( request, baseUrl );
        if ( request.isAsync() )
        {
            return new PathsPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( Exception e )
            {
                logger.error( "Path promotion failed: " + request.getSource() + " -> " + request.getTarget(), e );
                throw new PromotionException( "Execution of path promotion failed.", e );
            }
        }
    }

    private Future<PathsPromoteResult> submitPathsPromoteRequest( PathsPromoteRequest request, final String baseUrl )
    {
        return detectOverload( () -> promotionService.submit( () -> {
            PathsPromoteResult ret;
            try
            {
                ret = doPathsPromotion( request, false, baseUrl );
            }
            catch ( Exception ex )
            {
                String msg = "Path promotion failed. Target: " + request.getTarget() + ", Source: "
                                + request.getSource() + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new PathsPromoteResult( request, msg );
            }

            if ( ret.getRequest().getCallback() != null )
            {
                return callbackHelper.callback( ret.getRequest().getCallback(), ret );
            }

            return ret;
        } ) );
    }

    /**
     * Attempt to rollback a previous {@link PathsPromoteResult}.
     *
     * All paths in the completed paths set are deleted from the target. The output {@link PathsPromoteResult} contains
     * the previous content, with removed target paths moved from the completed list to the pending list.
     *
     * @param result The result to rollback
     * @return The same result, with successful deletions moved from the completed to pending paths list,
     * and the error cleared (or set to a new error)
     */
    public PathsPromoteResult rollbackPathsPromote( final PathsPromoteResult result )
                    throws PromotionException
    {
        final PathsPromoteRequest request = result.getRequest();

        Future<PathsPromoteResult> future = submitRollbackPathsPromote( result );
        if ( request.isAsync() )
        {
            return new PathsPromoteResult( request ).accepted();
        }
        else
        {
            try
            {
                return future.get();
            }
            catch ( Exception e )
            {
                logger.error( "Path promotion rollback failed. From (target): " + request.getTarget()
                                              + ", to (source): " + request.getSource(), e );
                throw new PromotionException( "Path promotion rollback failed.", e );
            }
        }
    }

    private Future<PathsPromoteResult> submitRollbackPathsPromote( PathsPromoteResult result )
    {
        return detectOverload( () -> promotionService.submit( () -> {
            if ( result.getCompletedPaths().isEmpty() )
            {
                // clear errors so client don't misunderstand rollback result
                logger.info( "Nothing to rollback (completed empty), promotionId: {}",
                             result.getRequest().getPromotionId() );
                result.setError( null );
                result.setValidations( null );
                return result;
            }

            /*
             * Rollback is the opposite of promotion. We just revert the src and target, set the paths
             * to the completed, and call the doPathsPromotion.
             */
            PathsPromoteRequest request = result.getRequest();
            PathsPromoteRequest newRequest = new PathsPromoteRequest( request.getTarget(), request.getSource(),
                                                                      result.getCompletedPaths() );
            newRequest.setPurgeSource( true );

            PathsPromoteResult ret;
            try
            {
                ret = doPathsPromotion( newRequest, true, null );
            }
            catch ( Exception ex )
            {
                String msg = "Rollback path promotion failed. Target: " + request.getTarget() + ", Source: "
                                + request.getSource() + ", Reason: " + getStackTrace( ex );
                logger.warn( msg );
                ret = new PathsPromoteResult( request, msg );
            }

            if ( ret.succeeded() )
            {
                result.setPendingPaths( result.getCompletedPaths() );
                result.setCompletedPaths( null );
            }
            else
            {
                result.setError( ret.getError() );
            }

            if ( result.getRequest().getCallback() != null )
            {
                return callbackHelper.callback( result.getRequest().getCallback(), result );
            }

            return result;
        } ) );
    }

    private PathsPromoteResult doPathsPromotion( PathsPromoteRequest request, boolean skipValidation, String baseUrl )
                    throws Exception
    {
        Set<String> paths = request.getPaths();
        StoreKey source = request.getSource();
        logger.info( "Do paths promotion, promotionId: {}, source: {}, target: {}, size: {}", request.getPromotionId(),
                     source, request.getTarget(), paths != null ? paths.size() : -1 );

        // If no paths in the request, we query storage service to list all non-metadata files
        if ( paths == null || paths.isEmpty() )
        {
            Response resp = storageService.list(source.toString(), true, TYPE_FILE, 0); // no limit
            if ( Response.Status.fromStatusCode( resp.getStatus() ).getFamily()
                    != Response.Status.Family.SUCCESSFUL  )
            {
                return new PathsPromoteResult( request,"List source repo failed, resp status: " + resp.getStatus() );
            }
            String[] listResult = (String[]) resp.getEntity();

            paths = Arrays.stream(listResult).sequential()
                    .filter(isMetadataPredicate().negate()).collect(toSet()); // exclude metadata
            request.setPaths( paths );
            logger.info( "List source repo, paths: {}", paths );
        }

        // Always skip metadata
        final Set<String> skippedMetadata = request.getPaths().stream().filter(isMetadataPredicate()).collect(toSet());

        final Set<String> pending = request.getPaths().stream()
                .filter(isMetadataPredicate().negate()).collect(toSet());
        if ( pending.isEmpty() )
        {
            return new PathsPromoteResult( request, pending, emptySet(), skippedMetadata, null );
        }


        AtomicReference<Exception> ex = new AtomicReference<>();
        StoreKeyPaths plk = new StoreKeyPaths( request.getTarget(), pending );

        final PathsPromoteResult promoteResult;
        if ( request.isFailWhenExists() )
        {
            promoteResult = conflictManager.checkAnd( plk,
                    pathsLockKey -> runValidationAndPathPromotions( skipValidation, request, baseUrl, ex, pending ),
                    pathsLockKey -> {
                        String msg = String.format( "Conflict detected, store: %s, paths: %s", pathsLockKey.getTarget(), pending );
                        logger.warn( msg );
                        return new PathsPromoteResult( request, pending, emptySet(), emptySet(), msg, null );
                    } );
        }
        else
        {
            promoteResult = runValidationAndPathPromotions( skipValidation, request, baseUrl, ex, pending );
        }

        if ( ex.get() != null )
        {
            throw ex.get();
        }

        // purge only if all paths were promoted successfully
        if ( promoteResult != null && promoteResult.succeeded() && request.isPurgeSource() )
        {
            promotionHelper.purgeSourceQuietly( request.getSource(), pending );
        }

        // merge all skipped paths
        if ( !skippedMetadata.isEmpty() )
        {
            Set<String> allSkipped = new HashSet<>();
            allSkipped.addAll(skippedMetadata);
            allSkipped.addAll(promoteResult.getSkippedPaths());
            promoteResult.setSkippedPaths( allSkipped );
        }
        return promoteResult;
    }

    private PathsPromoteResult runValidationAndPathPromotions( boolean skipValidation, PathsPromoteRequest request,
                                                               String baseUrl, AtomicReference<Exception> ex,
                                                               Set<String> pending )
    {
        ValidationResult validationResult = null;
        if ( !skipValidation )
        {
            try
            {
                validationResult = validator.validate( request, baseUrl );
            }
            catch ( Exception e )
            {
                ex.set( e );
                return null;
            }
        }

        if ( validationResult == null || validationResult.isValid() )
        {
            if ( request.isDryRun() )
            {
                return new PathsPromoteResult( request, pending, emptySet(), emptySet(), validationResult );
            }
            PathsPromoteResult result = runPathPromotions( request, pending, validationResult );
            if ( !result.succeeded() )
            {
                logger.info( "Path promotion failed. Result: " + result );
            }
            else
            {
                logger.info( "Path promotion succeeded. Result: " + result );
                kafkaEventDispatcher.fireEvent( new PathsPromoteCompleteEvent(request.getPromotionId(),
                        request.getSource().toString(), request.getTarget().toString(), result.getCompletedPaths() ) );
            }
            return result;
        }

        // Validation failed
        return new PathsPromoteResult( request, pending, emptySet(), emptySet(), validationResult );
    }

    private PathsPromoteResult runPathPromotions( final PathsPromoteRequest request, final Set<String> pending,
                                                  final ValidationResult validation )
    {
        long begin = System.currentTimeMillis();

        PromotionHelper.PromotionRepoRetrievalResult checkResult = promotionHelper.checkAndRetrieveSourceAndTargetRepos( request );
        if ( checkResult.hasErrors() )
        {
            return new PathsPromoteResult( request, pending, emptySet(), emptySet(),
                                           StringUtils.join( checkResult.errors, "\n" ), validation );
        }

        // re-download missing remote files if any (this may be caused by reasons such as file expiration)
        if ( request.getSource().getType() == StoreType.remote )
        {
            reDownloadMissing(request.getSource(), pending);
        }

        final Set<PathTransferResult> results = copy( request );

        final List<String> errors = new ArrayList<>();
        final Set<String> completed = new HashSet<>();
        final Set<String> skipped = new HashSet<>();

        results.forEach( result -> {
            if ( result.error != null )
            {
                errors.add( result.error );
            }
            else if ( result.skipped )
            {
                skipped.add( result.path );
            }
            else
            {
                completed.add( result.path );
            }
        } );

        PathsPromoteResult result;
        if ( !errors.isEmpty() )
        {
            Set<String> rollbackErrors = promotionHelper.delete( request.getTarget(), completed );
            errors.addAll( rollbackErrors );
            result = new PathsPromoteResult( request, pending, emptySet(), emptySet(), StringUtils.join( errors, "\n" ),
                                             validation );
        }
        else
        {
            result = new PathsPromoteResult( request, emptySet(), completed, skipped, null, validation );
/*
 * TODO: dump this NFC clean up. On promote service, we have no NFC cache involved.
 * Instead, we should fire event via Kafka telling that some NFC entries and maven matedata files should be expired,
 * as consequences of the promotion.

            final String name = String.format( "PromoteNFCClean-method(%s)-source(%s)-target(%s)", "runPathPromotions",
                                               request.getSource(), targetStore.getKey() );
            final String context =
                    String.format( "Class: %s, method: %s, source: %s, target: %s", this.getClass().getName(),
                                   "runPathPromotions", request.getSource(), targetStore.getKey() );
            storeManager.asyncGroupAffectedBy( new StoreDataManager.ContextualTask( name, context,
                                                                                    () -> promotionHelper.clearStoreNFC(
                                                                                                    completed,
                                                                                                    targetStore,
                                                                                                    affectedGroups ) ) );
            if ( request.isFireEvents() )
            {
                fireEvent( promoteCompleteEvent, new PathsPromoteCompleteEvent( result ) );
            }
*/
        }

        logger.info( "Promotion completed, promotionId: {}, timeInSeconds: {}", request.getPromotionId(),
                     timeInSeconds( begin ) );
        return result;
    }

    private boolean reDownloadMissing(StoreKey storeKey, Set<String> paths)
    {
        BatchExistRequest request = new BatchExistRequest();
        request.setFilesystem( storeKey.toString());
        request.setPaths(paths);
        Response resp = storageService.exist(request);
        if (!isSuccess(resp))
        {
            logger.warn("Re-download existence check failed, status: {}", resp.getStatus() );
            return false;
        }
        BatchExistResult batchExistResult = resp.readEntity(BatchExistResult.class);
        Set<String> missing = batchExistResult.getMissing();
        if (missing != null && !missing.isEmpty())
        {
            logger.info("Re-download missing, storeKey: {}, paths: {}", storeKey, missing);
            missing.forEach( p -> contentService.retrieve(
                    storeKey.getPackageType(), storeKey.getType().getName(), storeKey.getName(), p));
        }
        return true;
    }

    private Set<PathTransferResult> copy(PathsPromoteRequest promoteRequest)
    {
        FileCopyRequest fileCopyRequest = new FileCopyRequest();
        fileCopyRequest.setFailWhenExists( promoteRequest.isFailWhenExists() );
        fileCopyRequest.setSourceFilesystem( promoteRequest.getSource().toString() );
        fileCopyRequest.setTargetFilesystem( promoteRequest.getTarget().toString() );
        fileCopyRequest.setPaths( promoteRequest.getPaths() );

        Response resp = storageService.copy(fileCopyRequest);
        if (!isSuccess(resp))
        {
            return errResults( "Copy failed, resp status: " + resp.getStatus() );
        }
        FileCopyResult fileCopyResult = resp.readEntity(FileCopyResult.class);

        if ( fileCopyResult.isSuccess() )
        {
            Set<PathTransferResult> results = new HashSet<>();
            Set<String> completed = fileCopyResult.getCompleted();
            if ( completed != null )
            {
                completed.forEach( p -> results.add(new PathTransferResult(p, false, null)));
            }
            Set<String> skipped = fileCopyResult.getSkipped();
            if ( skipped != null )
            {
                skipped.forEach( p -> results.add(new PathTransferResult(p, true, null)));
            }
            return results;
        }

        // Otherwise return error
        return errResults( "Copy failed: " + fileCopyResult.getMessage() );
    }

    private boolean isSuccess(Response resp) {
        return Response.Status.fromStatusCode(resp.getStatus()).getFamily()
                == Response.Status.Family.SUCCESSFUL;
    }

    private Set<PathTransferResult> errResults(String errors)
    {
        PathTransferResult errResult = new PathTransferResult();
        errResult.error = errors;
        Set<PathTransferResult> errResults = new HashSet<>( 1 );
        errResults.add( errResult );
        return errResults;
    }

/*
    private Callable<Set<PathTransferResult>> newPathPromotionsJob( final Collection<Transfer> transfers,
                                                                    final ArtifactStore tgt,
                                                                    final PathsPromoteRequest request,
                                                                    final Set<Group> affectedGroups )
    {
        return () -> {
            Set<String> pathsForMDC = new HashSet<>();
            Set<PathTransferResult> results = new HashSet<>();
            for ( Transfer transfer : transfers )
            {
                pathsForMDC.add( transfer.getPath() );

                PathTransferResult ret = doPathTransfer( transfer, tgt, request, affectedGroups );
                results.add( ret );
            }
            RequestContextHelper.setContext( PROMOTION_CONTENT_PATH, pathsForMDC.toString() );
            return results;
        };
    }

    private PathTransferResult doPathTransfer( Transfer transfer, final ArtifactStore tgt,
                                               final PathsPromoteRequest request, final Set<Group> affectedGroups )
                    throws Exception
    {
        logger.debug( "Do path transfer, transfer: {}, target: {}", transfer, tgt );

        if ( transfer == null )
        {
            final String error = String.format( "Warning: doPathTransfer cannot process null transfer to target: %s", tgt );
            logger.error( error );
            //FIXME: throw IndyWorkflowException is better?
            PathTransferResult result = new PathTransferResult( "" );
            result.error = error;
            return result;
        }

        long begin = System.currentTimeMillis();

        final String path = transfer.getPath();
        PathTransferResult result = new PathTransferResult( path );

        if ( !transfer.exists() )
        {
            logger.debug( "Transfer not exist, {}", transfer );
            SpecialPathInfo pathInfo = specialPathManager.getSpecialPathInfo( transfer, tgt.getPackageType() );
            // if we can't decorate it, that's because we don't want to automatically generate checksums, etc. for it
            // i.e. it's something we would generate on demand for another file.
            if ( pathInfo != null && !pathInfo.isDecoratable() )
            {
                logger.info( "Skipping missing, not decoratable path: {}", transfer );
                result.skipped = true;
                return result;
            }

            if ( promotionHelper.isRemoteTransfer( transfer ) )
            {
                transfer = promotionHelper.redownload( transfer ); // try re-download it for remote artifacts
            }

            if ( transfer == null || !transfer.exists() )
            {
                String msg = String.format( "Failed to promote: %s. Source file not exists.", transfer );
                logger.info( msg );
                result.error = msg;
                return result;
            }
        }

        Transfer target = contentManager.getTransfer( tgt, path, UPLOAD );
        EventMetadata eventMetadata = new EventMetadata().set( IGNORE_READONLY, true );

        // if we hit an existing metadata.xml, we remove it from both target repo and affected groups. The metadata
        // will be regenerated on next request.
        SpecialPathInfo pathInfo = specialPathManager.getSpecialPathInfo( target, tgt.getPackageType() );
        if ( pathInfo != null && pathInfo.isMetadata() )
        {
            try
            {
                if ( target != null && target.exists() )
                {
                    contentManager.delete( tgt, path, eventMetadata );
                }
                result.skipped = true;
                logger.info( "Metadata, mark as skipped and remove it if exists, target: {}", target );
            }
            catch ( Exception e )
            {
                String msg = String.format( "Failed to promote metadata: %s. Target: %s. Error: %s", transfer,
                                            request.getTarget(), e.getMessage() );
                logger.error( msg, e );
                result.error = msg;
            }

            return result;
        }

        if ( target != null && target.exists() )
        {
            // e.g., fail in case of promotion of built artifacts into pnc-builds while it should pass (skip them)
            // in case of promotion of dependencies into shared-imports.
            if ( request.isFailWhenExists() )
            {
                String msg = String.format( "Failed to promote: %s. Target: %s. Target file already exists.",
                                            transfer, request.getTarget() );
                logger.info( msg );
                result.error = msg;
            }
            else
            {
                result.skipped = true;
            }
            return result;
        }

        logger.debug( "Store target transfer: {}", target );
        eventMetadata.set( AFFECTED_GROUPS, new ValuePipe<>( affectedGroups ) ).set( TARGET_STORE, tgt );

        try (InputStream stream = transfer.openInputStream( true ))
        {
            contentManager.store( tgt, path, stream, UPLOAD, eventMetadata );
        }
        catch ( final IOException e )
        {
            String msg = String.format( "Failed to promote: %s. Error: %s", transfer, e.getMessage() );
            result.error = msg;
            logger.error( msg, e );
        }

        logger.info( "Promotion transfer completed, target: {}, path: {}, timeInMillSeconds: {}", tgt.getKey(), path,
                     timeInMillSeconds( begin ) );

        return result;
    }
*/

}
