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
package org.commonjava.service.promote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;

import org.commonjava.service.promote.client.repository.RepositoryService;
import org.commonjava.service.promote.config.TestPromoteConfig;
import org.commonjava.service.promote.core.ContentDigester;
import org.commonjava.service.promote.core.PromotionManager;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.commonjava.service.promote.util.PromoteDataFileManager;
import org.commonjava.service.promote.util.ScriptEngine;
import org.commonjava.service.promote.validate.PromoteValidationsManager;
import org.commonjava.service.promote.validate.PromotionValidationTools;
import org.commonjava.service.promote.validate.PromotionValidator;
import org.commonjava.service.promote.validate.ValidationRuleParser;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.*;

import static org.commonjava.service.promote.model.pkg.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class PromotionManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Inject
    @RestClient
    RepositoryService repositoryService;

    private PromotionManager promotionManager;

    private PromoteDataFileManager promoteDataManager;

    private PromoteValidationsManager validationsManager;

    private PromotionValidator validator;

    private Executor executor;

    private static final String FAKE_BASE_URL = "";

    private TestPromoteConfig config;

    @Before
    public void setup()
            throws Exception
    {

        File base = temp.newFolder("data" );
        config = new TestPromoteConfig();
        config.setBaseDir( base );
/*
        contentMetadata.clear();

        galleyParts = new GalleyMavenFixture( true, temp );
        galleyParts.initMissingComponents();

        storeManager = new MemoryStoreDataManager( true );

        final DefaultIndyConfiguration indyConfig = new DefaultIndyConfiguration();
        indyConfig.setNotFoundCacheTimeoutSeconds( 1 );
        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( indyConfig );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        downloadManager = new DefaultDownloadManager( storeManager, galleyParts.getTransferManager(),
                                                      new IndyLocationExpander( storeManager ),
                                                      new MockInstance<>( new MockContentAdvisor() ), nfc, rescanService );

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        DirectContentAccess dca =
                new DefaultDirectContentAccess( downloadManager, contentAccessService );
*/

        ContentDigester contentDigester = new ContentDigester();

/*
        specialPathManager = new SpecialPathManagerImpl();

        contentManager = new DefaultContentManager( storeManager, downloadManager, new IndyObjectMapper( true ),
                                                    specialPathManager, new MemoryNotFoundCache(),
                                                    contentDigester, new ContentGeneratorManager() );
*/

        File tempData = temp.newFolder( "data" );
        promoteDataManager = new PromoteDataFileManager( config );
        validationsManager = new PromoteValidationsManager( promoteDataManager, config,
                                                            new ValidationRuleParser( new ScriptEngine(),
                                                                                      new ObjectMapper() ) );

        WeftExecutorService validateService =
                        new PoolWeftExecutorService( "test-validate-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
/*
        MavenModelProcessor modelProcessor = new MavenModelProcessor();

        PromoteConfig config = new PromoteConfig();
*/
        validator = new PromotionValidator( validationsManager,
                                            new PromotionValidationTools( contentDigester, null, config ),
                                            validateService );

        WeftExecutorService svc = new PoolWeftExecutorService( "test-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(),
                        2, 10f, false,null, null );

        promotionManager = new PromotionManager( validator, config, svc );

        executor = Executors.newCachedThreadPool();
    }

    /**
     * On collision, the promotion manager should skip the second file to be promoted (instead of overwriting the
     * existing one). This assumes no overwrite attribute is available in the promotion request (defaults to false).
     */
    @Test
    public void promoteAllByPath_CollidingPaths_VerifySecondSkipped()
            throws Exception
    {
        StoreKey source1 = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "source1" );
        StoreKey source2 = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "source2" );
/*
        final HostedRepository source1 = new HostedRepository( MAVEN_PKG_KEY,  "source1" );
        final HostedRepository source2 = new HostedRepository( MAVEN_PKG_KEY,  "source2" );
*/
        repositoryService.putStore( source1.getPackageType(), source1.getType().singularEndpointName(), source1.getName() );
        repositoryService.putStore( source2.getPackageType(), source2.getType().singularEndpointName(), source2.getName()  );

        String originalString = "This is a test";

        final String path = "/path/path";

/*
        contentManager.store( source1, path, new ByteArrayInputStream( originalString.getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source2, path, new ByteArrayInputStream( "This is another test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
*/
        // TODO: Save above files to storage service
/*
        final HostedRepository target = new HostedRepository( MAVEN_PKG_KEY,  "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );
*/

        repositoryService.putStore( MAVEN_PKG_KEY, StoreType.hosted.singularEndpointName(), "target" );

        PathsPromoteResult result =
                promotionManager.promotePaths( new PathsPromoteRequest( source1, target, path ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source1 ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped == null || skipped.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );

        assertThat( result.getError(), nullValue() );

/*
        Transfer ref = downloadManager.getStorageReference( target, path );
        assertThat( ref.exists(), equalTo( true ) );
        try (InputStream in = ref.openInputStream())
        {
            String value = IOUtils.toString( in );
            assertThat( value, equalTo( originalString ) );
        }
*/
        //TODO: verify target file exists with content

        result = promotionManager.promotePaths( new PathsPromoteRequest( source1, target, path ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source1 ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( 1 ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        assertThat( result.getError(), nullValue() );

/*        ref = downloadManager.getStorageReference( target, path );
        assertThat( ref.exists(), equalTo( true ) );
        try (InputStream in = ref.openInputStream())
        {
            String value = IOUtils.toString( in );
            assertThat( value, equalTo( originalString ) );
        }*/

        // TODO: verify it...
    }

/*
    @Test
    @Ignore( "volatile, owing to galley fs locks")
    public void promoteAllByPath_RaceToPromote_FirstLocksTargetStore()
            throws Exception
    {
        SecureRandom rand = new SecureRandom();
        final HostedRepository[] sources = { new HostedRepository( MAVEN_PKG_KEY,  "source1" ), new HostedRepository( MAVEN_PKG_KEY,  "source2" ) };
        final String[] paths = { "/path/path1", "/path/path2", "/path3", "/path/path/4" };
        Stream.of( sources ).forEach( ( source ) ->
                                      {
                                          try
                                          {
                                              storeManager.storeArtifactStore( source, new ChangeSummary(
                                                                                       ChangeSummary.SYSTEM_USER, "test setup" ), false, true,
                                                                               new EventMetadata() );

                                              Stream.of( paths ).forEach( ( path ) ->
                                                                          {
                                                                              byte[] buf = new byte[1024 * 1024 * 2];
                                                                              rand.nextBytes( buf );
                                                                              try
                                                                              {
                                                                                  contentManager.store( source, path,
                                                                                                        new ByteArrayInputStream(
                                                                                                                buf ),
                                                                                                        TransferOperation.UPLOAD,
                                                                                                        new EventMetadata() );
                                                                              }
                                                                              catch ( IndyWorkflowException e )
                                                                              {
                                                                                  Assert.fail(
                                                                                          "failed to store generated file to: "
                                                                                                  + source + path );
                                                                              }
                                                                          } );
                                          }
                                          catch ( IndyDataException e )
                                          {
                                              Assert.fail( "failed to store hosted repository: " + source );
                                          }
                                      } );

        final HostedRepository target = new HostedRepository( MAVEN_PKG_KEY,  "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );

        PathsPromoteResult[] results = new PathsPromoteResult[2];
        CountDownLatch cdl = new CountDownLatch( 2 );

        AtomicInteger counter = new AtomicInteger( 0 );
        Stream.of( sources ).forEach( ( source ) ->
                                      {
                                          int idx = counter.getAndIncrement();
                                          executor.execute( () ->
                                                            {
                                                                try
                                                                {
                                                                    results[idx] = manager.promotePaths(
                                                                            new PathsPromoteRequest( source.getKey(),
                                                                                                     target.getKey(),
                                                                                                     paths ),
                                                                            FAKE_BASE_URL );
                                                                }
                                                                catch ( Exception e )
                                                                {
                                                                    Assert.fail( "Promotion from source: " + source
                                                                                         + " failed." );
                                                                }
                                                                finally
                                                                {
                                                                    cdl.countDown();
                                                                }
                                                            } );

                                          try
                                          {
                                              Thread.sleep( 25 );
                                          }
                                          catch ( InterruptedException e )
                                          {
                                              Assert.fail( "Test interrupted" );
                                          }
                                      } );

        assertThat( "Promotions failed to finish.", cdl.await( 30, TimeUnit.SECONDS ), equalTo( true ) );

        // first one should succeed.
        PathsPromoteResult result = results[0];
        assertThat( result.getRequest().getSource(), equalTo( sources[0].getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped == null || skipped.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( paths.length ) );

        assertThat( result.getError(), nullValue() );

        Stream.of( paths ).forEach( ( path ) ->
                                    {
                                        HostedRepository src = sources[0];
                                        Transfer sourceRef = downloadManager.getStorageReference( src, path );
                                        Transfer targetRef = downloadManager.getStorageReference( target, path );
                                        assertThat( targetRef.exists(), equalTo( true ) );
                                        try (InputStream sourceIn = sourceRef.openInputStream();
                                             InputStream targetIn = targetRef.openInputStream())
                                        {
                                            int s, t;
                                            while ( ( s = sourceIn.read() ) == targetIn.read()  )
                                            {
                                                if ( s == -1 )
                                                {
                                                    break;
                                                }
                                            }

                                            if ( s != -1 )
                                            {
                                                Assert.fail(
                                                        path + " doesn't match between source: " + src + " and target: "
                                                                + target );
                                            }
                                        }
                                        catch ( IOException e )
                                        {
                                            Assert.fail(
                                                    "Failed to compare contents of: " + path + " between source: " + src
                                                            + " and target: " + target );
                                        }
                                    } );

        // second one should be completely skipped.
        result = results[1];
        assertThat( result.getRequest().getSource(), equalTo( sources[1].getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( paths.length ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        assertThat( result.getError(), nullValue() );
    }
*/

    @Test
    public void promoteAllByPath_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result =
                promotionManager.promotePaths( new PathsPromoteRequest( source, target ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( true, true, true, true );
    }

    @Test
    public void promoteAllByPath_PushTwoArtifactsToHostedRepo_DryRun_VerifyPendingPathsPopulated()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result =
                promotionManager.promotePaths( new PathsPromoteRequest( source, target ).setDryRun( true ),
                                      FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    @Test
    public void promoteAllByPath_PurgeSource_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result = promotionManager.promotePaths(
                new PathsPromoteRequest( source, target ).setPurgeSource( true ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( true, true, false, false );
    }

    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        PathsPromoteResult result =
                promotionManager.promotePaths( new PathsPromoteRequest( source, target ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( "should be null or empty: " + pending, pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = promotionManager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    @Test
    public void rollback_PurgeSource_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback_VerifyContentInSource()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        PathsPromoteResult result = promotionManager.promotePaths(
                new PathsPromoteRequest( source, target ).setPurgeSource( true ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = promotionManager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source ) );
        assertThat( result.getRequest().getTarget(), equalTo( target ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    /**
     * To make the promotion fail, we just add a same path to target and set the request failWhenExists.
     */
    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteFailedAndAutoRollback() throws Exception
    {
        prepareHostedReposAndTwoPaths();

/*
        contentManager.store( target, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
*/
        // TODO: store file by storage service

        PathsPromoteRequest request = new PathsPromoteRequest( source, target );
        request.setFailWhenExists( true );

        PathsPromoteResult result = promotionManager.promotePaths( request, FAKE_BASE_URL );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed.size(), equalTo( 0 ) );

        assertThat( result.getError(), notNullValue() );
        System.out.println( ">>> " + result.getError() );

        verifyExistence( false, true, true, true );
    }


    private StoreKey source, target;

    private final String first = "/first/path";

    private final String second = "/second/path";

    private void prepareHostedReposAndTwoPaths() throws Exception
    {
        prepareHostedRepos();

/*
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
*/
    }

    private void prepareHostedRepos() throws Exception
    {
/*
        source = new HostedRepository( MAVEN_PKG_KEY, "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ), false,
                                         true, new EventMetadata() );
        target = new HostedRepository( MAVEN_PKG_KEY, "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ), false,
                                         true, new EventMetadata() );
*/
    }

    private void verifyExistence( boolean tgtFirst, boolean tgtSecond, boolean srcFirst, boolean srcSecond )
    {
/*
        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( tgtFirst ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( tgtSecond ) );

        ref = downloadManager.getStorageReference( source, first );
        assertThat( ref.exists(), equalTo( srcFirst ) );

        ref = downloadManager.getStorageReference( source, second );
        assertThat( ref.exists(), equalTo( srcSecond ) );
*/
    }
}
