/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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

import groovy.lang.Closure;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.pkg.npm.content.PackagePath;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.service.promote.client.content.ContentService;
import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.core.ContentDigester;
import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.util.ContentDigest;
import org.commonjava.service.promote.util.ResponseHelper;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_OK;
import static org.commonjava.service.promote.util.Batcher.batch;
import static org.commonjava.service.promote.util.Batcher.getParalleledBatchSize;

@ApplicationScoped
public class PromotionValidationTools
{
    final Logger logger = LoggerFactory.getLogger( this.getClass() );

    public static final String AVAILABLE_IN_STORES = "availableInStores";

    @Deprecated
    public static final String AVAILABLE_IN_STORE_KEY = "availableInStoreKey";

    private static final int DEFAULT_RULE_PARALLEL_WAIT_TIME_MINS = 30;

    //private static final String ITERATION_DEPTH = "promotion-validation-parallel-depth";

    //private static final String ITERATION_ITEM = "promotion-validation-parallel-item";

    @Inject
    ResponseHelper responseHelper;

    @Inject
    ContentDigester contentDigester;

    @Inject
    PromoteConfig promoteConfig;

    @Inject
    IndyObjectMapper objectMapper;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "promote-rules-batch-executor", threads = 16 )
    WeftExecutorService ruleParallelExecutor;

    @Inject
    @RestClient
    ContentService contentService;

    public PromotionValidationTools()
    {
    }

    public PromotionValidationTools( final ContentDigester contentDigester,
                                     final ThreadPoolExecutor ruleParallelExecutor, final PromoteConfig config )
    {
        this.contentDigester = contentDigester;
        this.ruleParallelExecutor = new PoolWeftExecutorService( "promote-rules-batch-executor", ruleParallelExecutor );
        this.promoteConfig = config;
    }

    public StoreKey[] getValidationStoreKeys(final ValidationRequest request )
            throws PromotionValidationException
    {
        return getValidationStoreKeys( request, false );
    }

    /**
     * @deprecated This method now is only used for api backward compatible
     */
    @Deprecated
    public StoreKey[] getValidationStoreKeys( final ValidationRequest request, final boolean includeSource,
                                              final boolean includeTarget )
            throws PromotionValidationException
    {
        return getValidationStoreKeys( request, includeSource );
    }

    public StoreKey[] getValidationStoreKeys( final ValidationRequest request, final boolean includeSource )
            throws PromotionValidationException
    {
        String verifyStores = request.getValidationParameter( AVAILABLE_IN_STORES );
        if ( verifyStores == null )
        {
            verifyStores = request.getValidationParameter( AVAILABLE_IN_STORE_KEY );
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Got extra validation keys string: '{}'", verifyStores );

        Set<StoreKey> verifyStoreKeys = new HashSet<>();
        if ( includeSource )
        {
            verifyStoreKeys.add( request.getSource() );
        }


        if ( verifyStores == null )
        {
            logger.warn(
                    "No external store (availableInStoreKey parameter) specified for validating path availability in rule-set: {}. Using target: {} instead.",
                    request.getRuleSet().getName(), request.getTarget() );
        }
        else
        {
            List<StoreKey> extras = Stream.of( verifyStores.split( "\\s*,\\s*" ) )
                    .map( StoreKey::fromString )
                    .filter( item -> item != null )
                    .collect( Collectors.toList() );

            if ( extras.isEmpty() )
            {
                throw new PromotionValidationException( "No valid instances could be parsed from " + verifyStores );
            }
            else
            {
                verifyStoreKeys.addAll( extras );
            }
        }

        if ( verifyStoreKeys.isEmpty() )
        {
            verifyStoreKeys.add( request.getTarget() );
        }

        logger.debug( "Using validation StoreKeys: {}", verifyStoreKeys );

        return verifyStoreKeys.toArray( new StoreKey[0] );
    }

/*
    public String toArtifactPath( final ProjectVersionRef ref )
            throws TransferException
    {
        return ArtifactPathUtils.formatArtifactPath( ref, typeMapper );
    }

    public String toMetadataPath( final ProjectRef ref, final String filename )
            throws TransferException
    {
        return ArtifactPathUtils.formatMetadataPath( ref, filename );
    }

    public String toMetadataPath( final String groupId, final String filename )
            throws TransferException
    {
        return ArtifactPathUtils.formatMetadataPath( groupId, filename );
    }

    public Set<ProjectRelationship<?, ?>> getRelationshipsForPom( final String path, final ModelProcessorConfig config,
                                                                  final ValidationRequest request,
                                                                  final StoreKey... extraLocations )
            throws Exception
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Retrieving relationships for POM: {} (using extra locations: {})", path,
                Arrays.asList( extraLocations ) );

        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            logger.trace( "{} is not a valid artifact reference. Skipping.", path );
            return null;
        }

        StoreKey key = request.getSource();
        Transfer transfer = retrieve( request.getSource(), path );
        if ( transfer == null )
        {
            logger.trace( "Could not retrieve Transfer instance for: {} (path: {}, extra locations: {})", key, path,
                    Arrays.asList( extraLocations ) );
            return null;
        }

        List<Location> locations = new ArrayList<>( extraLocations.length + 1 );
        locations.add( transfer.getLocation() );
        addLocations( locations, extraLocations );

        MavenPomView pomView =
                pomReader.read( artifactRef.asProjectVersionRef(), transfer, locations, MavenPomView.ALL_PROFILES );

        try
        {
            URI source = new URI( "indy:" + key.getType().name() + ":" + key.getName() );

            return modelProcessor.readRelationships( pomView, source, config ).getAllRelationships();
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException(
                    "Failed to construct URI for ArtifactStore: " + key + ". Reason: " + e.getMessage(), e );
        }
    }

    public void addLocations( final List<Location> locations, final StoreKey... extraLocations )
            throws Exception
    {
        for ( StoreKey extra : extraLocations )
        {
            ArtifactStore store = getArtifactStore( extra );
            locations.add( LocationUtils.toLocation( store ) );
        }
    }

    public MavenPomView readPom( final String path, final ValidationRequest request, final StoreKey... extraLocations )
            throws Exception
    {
        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            return null;
        }

        Transfer transfer = retrieve( request.getSource(), path );

        List<Location> locations = new ArrayList<>( extraLocations.length + 1 );
        locations.add( transfer.getLocation() );
        addLocations( locations, extraLocations );

        return pomReader.read( artifactRef.asProjectVersionRef(), transfer, locations, MavenPomView.ALL_PROFILES );
    }
*/

    public void readLocalPom( final String path, final ValidationRequest request )
            throws Exception
    {
        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            throw new Exception( String.format("Invalid artifact path: %s. Could not parse ArtifactRef from path.", path) );
        }
        StoreKey src = request.getSource();
        Response resp = contentService.retrieve(src.getPackageType(), src.getType().getName(), src.getName(), path);
        if ( resp.getStatus() == SC_OK ) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            reader.read(resp.readEntity(InputStream.class));
            return;
        }
        throw new Exception( String.format("File not exist, srcFilesystem: %s, path: %s", src, path) );
    }

    public PackageMetadata readLocalPackageJson(final String path, final ValidationRequest request )
            throws Exception
    {
        StoreKey src = request.getSource();
        Response resp = contentService.retrieve(src.getPackageType(), src.getType().getName(), src.getName(), path);
        if ( resp.getStatus() == SC_OK ) {
            try (InputStream is = resp.readEntity( InputStream.class ))
            {
                return objectMapper.readValue( is, PackageMetadata.class );
            }
        }
        else
        {
            throw new Exception(
                    String.format("Invalid artifact path: %s. Could not parse package metadata from path.", path ));
        }
    }

    public ArtifactRef getArtifact( final String path )
    {
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        return pathInfo == null ? null : pathInfo.getArtifact();
    }

    public Optional<PackagePath> getNPMPackagePath(final String tarPath )
    {
        return PackagePath.parse( tarPath );
    }
/*

    public MavenMetadataView getMetadata( final ProjectRef ref, final List<? extends Location> locations )
            throws GalleyMavenException
    {
        return metadataReader.getMetadata( ref, locations );
    }

    public MavenMetadataView readMetadata( final ProjectRef ref, final List<Transfer> transfers )
            throws GalleyMavenException
    {
        return metadataReader.readMetadata( ref, transfers );
    }

    public MavenMetadataView getMetadata( final ProjectRef ref, final List<? extends Location> locations,
                                          final EventMetadata eventMetadata )
            throws GalleyMavenException
    {
        return metadataReader.getMetadata( ref, locations, eventMetadata );
    }

    public MavenMetadataView readMetadata( final ProjectRef ref, final List<Transfer> transfers,
                                           final EventMetadata eventMetadata )
            throws GalleyMavenException
    {
        return metadataReader.readMetadata( ref, transfers, eventMetadata );
    }

    public MavenPomView read( final ProjectVersionRef ref, final Transfer pom, final List<? extends Location> locations,
                              final String... activeProfileLocations )
            throws GalleyMavenException
    {
        return pomReader.read( ref, pom, locations, activeProfileLocations );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations,
                              final boolean cache, final EventMetadata eventMetadata, final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, cache, eventMetadata, activeProfileIds );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations,
                              final boolean cache, final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, cache, activeProfileIds );
    }

    public MavenPomView readLocalPom( final ProjectVersionRef ref, final Transfer transfer,
                                      final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, activeProfileIds );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations,
                              final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, activeProfileIds );
    }

    public MavenPomView readLocalPom( final ProjectVersionRef ref, final Transfer transfer, final boolean cache,
                                      final EventMetadata eventMetadata, final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, cache, eventMetadata, activeProfileIds );
    }

    public MavenPomView readLocalPom( final ProjectVersionRef ref, final Transfer transfer,
                                      final EventMetadata eventMetadata, final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, eventMetadata, activeProfileIds );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations,
                              final EventMetadata eventMetadata, final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, eventMetadata, activeProfileIds );
    }

    public MavenPomView readLocalPom( final ProjectVersionRef ref, final Transfer transfer, final boolean cache,
                                      final String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, cache, activeProfileIds );
    }

    public MavenPomView read( final ProjectVersionRef ref, final Transfer pom, final List<? extends Location> locations,
                              final EventMetadata eventMetadata, final String... activeProfileLocations )
            throws GalleyMavenException
    {
        return pomReader.read( ref, pom, locations, eventMetadata, activeProfileLocations );
    }

    public Transfer getTransfer( final List<ArtifactStore> stores, final String path )
            throws Exception
    {
        return readOnlyWrapper( contentManager.getTransfer( stores, path, TransferOperation.DOWNLOAD ) );
    }

    public Transfer getTransfer( final StoreKey storeKey, final String path )
            throws Exception
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Retrieving transfer for: {} in {}", path, storeKey );
        Transfer result = readOnlyWrapper( contentManager.getTransfer( storeKey, path, TransferOperation.DOWNLOAD ) );
        logger.info( "Result: {}", result );
        return result;
    }

    public Transfer getTransfer( final ArtifactStore store, final String path )
            throws Exception
    {
        return readOnlyWrapper( contentManager.getTransfer( store, path, TransferOperation.DOWNLOAD ) );
    }

    public Transfer retrieve( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws Exception
    {
        return readOnlyWrapper( contentManager.retrieve( store, path, eventMetadata ) );
    }

    public Transfer retrieve( final ArtifactStore store, final String path )
            throws Exception
    {
        return readOnlyWrapper( contentManager.retrieve( store, path ) );
    }

    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path,
                                       final EventMetadata eventMetadata )
            throws Exception
    {
        return readOnlyWrappers( contentManager.retrieveAll( stores, path, eventMetadata ) );
    }

    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
            throws Exception
    {
        return readOnlyWrappers( contentManager.retrieveAll( stores, path ) );
    }

    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path,
                                   final EventMetadata eventMetadata )
            throws Exception
    {
        return readOnlyWrapper( contentManager.retrieveFirst( stores, path, eventMetadata ) );
    }

    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
            throws Exception
    {
        return readOnlyWrapper( contentManager.retrieveFirst( stores, path ) );
    }
*/

    public boolean exists( final StoreKey store, final String path )
            throws Exception
    {
        try
        {
            Response resp = contentService.exists(store.getPackageType(), store.getType().getName(), store.getName(), path);
            return resp.getStatus() == SC_OK;
        }
        catch (Exception e)
        {
            if (responseHelper.isRest404Exception(e))
            {
                return false;
            }
            throw e;
        }
    }

/*
    public List<StoreResource> list( final ArtifactStore store, final String path )
            throws Exception
    {
        return contentManager.list( store, path );
    }

    public List<StoreResource> list( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws Exception
    {
        return contentManager.list( store, path, eventMetadata );
    }

    public List<StoreResource> list( final List<? extends ArtifactStore> stores, final String path )
            throws Exception
    {
        return contentManager.list( stores, path );
    }
*/
    public String digest( final StoreKey key, final String path, ContentDigest digest )
            throws Exception
    {
        return contentDigester.digest( key, path, digest );
    }
/*
    public HttpExchangeMetadata getHttpMetadata( final Transfer txfr )
            throws Exception
    {
        return contentManager.getHttpMetadata( txfr );
    }

    public HttpExchangeMetadata getHttpMetadata( final StoreKey storeKey, final String path )
            throws Exception
    {
        return contentManager.getHttpMetadata( storeKey, path );
    }

    public ArtifactStoreQuery<ArtifactStore> artifactStoreQuery()
            throws Exception
    {
        return storeDataManager.query();
    }

    public ArtifactStore getArtifactStore( final StoreKey key )
            throws Exception
    {
        return storeDataManager.getArtifactStore( key );
    }

    public Set<ArtifactStore> getAllArtifactStores()
            throws Exception
    {
        return storeDataManager.getAllArtifactStores();
    }

    public Transfer getTransfer( final StoreResource resource )
    {
        return transferManager.getCacheReference( resource );
    }
*/

    public <T> void paralleledEach( Collection<T> collection, Closure closure )
    {
        final Logger logger = LoggerFactory.getLogger( this.getClass() );
        logger.trace( "Exe parallel on collection {} with closure {}", collection, closure );
        runParallelAndWait( collection, closure, logger );
    }

    public <T> void paralleledEach( T[] array, Closure closure )
    {
        final Logger logger = LoggerFactory.getLogger( this.getClass() );
        logger.trace( "Exe parallel on array {} with closure {}", array, closure );
        runParallelAndWait( Arrays.asList( array ), closure, logger );
    }

    public <T> void paralleledInBatch( Collection<T> collection, Closure closure )
    {
        int batchSize = getParalleledBatchSize( collection.size(), ruleParallelExecutor.getCorePoolSize() );
        logger.trace( "Exe parallel on collection {} with closure {} in batch {}", collection, closure, batchSize );
        Collection<Collection<T>> batches = batch( collection, batchSize );
        runParallelInBatchAndWait( batches, closure, logger );
    }

    public <T> void paralleledInBatch( T[] array, Closure closure )
    {
        int batchSize = getParalleledBatchSize( array.length, ruleParallelExecutor.getCorePoolSize() );
        logger.trace( "Exe parallel on array {} with closure {} in batch {}", array, closure, batchSize );
        Collection<Collection<T>> batches = batch( Arrays.asList( array ), batchSize );
        runParallelInBatchAndWait( batches, closure, logger );
    }

    private <T> void runParallelInBatchAndWait( Collection<Collection<T>> batches, Closure closure, Logger logger )
    {
        final CountDownLatch latch = new CountDownLatch( batches.size() );
        batches.forEach( batch -> ruleParallelExecutor.execute( () -> {
            try
            {
                logger.trace( "The paralleled exe on batch {}", batch );
                batch.forEach( e -> {
                    //String depthStr = MDC.get( ITERATION_DEPTH );
                    //RequestContextHelper.setContext( ITERATION_DEPTH, depthStr == null ? "0" : String.valueOf( Integer.parseInt( depthStr ) + 1 ) );
                    //RequestContextHelper.setContext( ITERATION_ITEM, String.valueOf( e ) );
                    try
                    {
                        closure.call( e );
                    }
                    finally
                    {
                        //MDC.remove( ITERATION_ITEM );
                        //MDC.remove( ITERATION_DEPTH );
                    }
                } );
            }
            finally
            {
                latch.countDown();
            }
        } ) );

        waitForCompletion( latch );
    }

    private <T> void runParallelAndWait( Collection<T> runCollection, Closure closure, Logger logger )
    {
        Set<T> todo = new HashSet<>( runCollection );
        final CountDownLatch latch = new CountDownLatch( todo.size() );
        todo.forEach( e -> ruleParallelExecutor.execute( () -> {
            //String depthStr = MDC.get( ITERATION_DEPTH );
            //RequestContextHelper.setContext( ITERATION_DEPTH, depthStr == null ? "0" : String.valueOf( Integer.parseInt( depthStr ) + 1 ) );
            //RequestContextHelper.setContext( ITERATION_ITEM, String.valueOf( e ) );

            try
            {
                logger.trace( "The paralleled exe on element {}", e );
                closure.call( e );
            }
            finally
            {
                latch.countDown();
                //MDC.remove( ITERATION_ITEM );
                //MDC.remove( ITERATION_DEPTH );
            }
        } ) );

        waitForCompletion( latch );
    }

    private void waitForCompletion( CountDownLatch latch )
    {
        try
        {
            // true if the count reached zero and false if timeout
            boolean finished = latch.await( DEFAULT_RULE_PARALLEL_WAIT_TIME_MINS, TimeUnit.MINUTES );
            if ( !finished )
            {
                throw new RuntimeException( "Parallel execution timeout" );
            }
        }
        catch ( InterruptedException e )
        {
            logger.error( "Rule validation execution failed due to parallel running timeout for {} minutes",
                    DEFAULT_RULE_PARALLEL_WAIT_TIME_MINS );
        }
    }

    public <T> void forEach( Collection<T> collection, Closure closure )
    {
        logger.trace( "Exe on collection {} with closure {}", collection, closure );
        collection.forEach( closure::call );
    }

    public <T> void forEach( T[] array, Closure closure )
    {
        logger.trace( "Exe on array {} with closure {}", array, closure );
        Arrays.asList( array ).forEach( closure::call );
    }

}
