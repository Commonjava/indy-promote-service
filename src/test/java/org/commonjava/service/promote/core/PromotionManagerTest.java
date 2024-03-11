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
package org.commonjava.service.promote.core;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;

import static org.apache.http.HttpStatus.SC_OK;
import static org.commonjava.service.promote.fixture.MockStorageService.mockedStorageRootDir;
import static org.commonjava.service.promote.model.pkg.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class PromotionManagerTest
{

/*
    @Inject
    @RestClient
    RepositoryService repositoryService;
*/

    @Inject
    @RestClient
    StorageService storageService;

    @Inject
    PromotionManager promotionManager;

    private static final String FAKE_BASE_URL = "";

    @BeforeEach
    public void setup() throws IOException {
        FileUtils.deleteDirectory( new File(mockedStorageRootDir) );
    }

    @AfterEach
    public void teardown() {
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

        StoreKey target1 = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "target1" );
        String originalString = "This is a test";
        String nextString = "This is another test...";

        final String path = "/path/path";

        // Save files to mocked storage service
        storageService.put( source1.toString(), path, new ByteArrayInputStream( originalString.getBytes() ));
        storageService.put( source2.toString(), path, new ByteArrayInputStream( nextString.getBytes() ));

        // Promote
        PathsPromoteResult result =
                promotionManager.promotePaths( new PathsPromoteRequest( source1, target1, path ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source1 ) );
        assertThat( result.getRequest().getTarget(), equalTo( target1 ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped == null || skipped.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );

        assertThat( result.getError(), nullValue() );

        // Verify target file content
        verifyContent( target1, path, originalString);

        // Promote again, the path should be skipped
        result = promotionManager.promotePaths( new PathsPromoteRequest( source2, target1, path ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source2 ) );
        assertThat( result.getRequest().getTarget(), equalTo( target1 ) );

        pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( 1 ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        assertThat( result.getError(), nullValue() );

        // Verify target file content has NO change
        verifyContent( target1, path, originalString);
    }

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

        storageService.put(target.toString(), second, new ByteArrayInputStream( "This is a test".getBytes() ));

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


    // below are some shared test variables
    private StoreKey source = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "source" );
    private StoreKey target = new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "target" );

    private final String first = "/first/path";

    private final String second = "/second/path";

    private void prepareHostedReposAndTwoPaths() throws Exception
    {
        prepareHostedRepos();
        storageService.put(source.toString(), first, new ByteArrayInputStream( "This is a test".getBytes() ));
        storageService.put(source.toString(), second, new ByteArrayInputStream( "This is another test".getBytes() ));
    }

    private void prepareHostedRepos() throws Exception
    {
    }

    private void verifyExistence( boolean tgtFirst, boolean tgtSecond, boolean srcFirst, boolean srcSecond )
    {
        assertEquals(tgtFirst, storageService.exists( target.toString(), first ).getStatus() == SC_OK);
        assertEquals(tgtSecond, storageService.exists( target.toString(), second ).getStatus() == SC_OK);
        assertEquals(srcFirst, storageService.exists( source.toString(), first ).getStatus() == SC_OK);
        assertEquals(srcSecond, storageService.exists( source.toString(), second ).getStatus() == SC_OK);
    }

    private void verifyContent( StoreKey storeKey, String path, String expectedContent ) throws IOException {
        Response resp = storageService.retrieve(storeKey.toString(), path);
        assertThat( resp.getStatus(), equalTo( SC_OK ) );

        try (InputStream is = resp.readEntity( InputStream.class ))
        {
            String content = IOUtils.toString( is, Charset.defaultCharset() );
            assertEquals( expectedContent, content );
        }
    }
}
