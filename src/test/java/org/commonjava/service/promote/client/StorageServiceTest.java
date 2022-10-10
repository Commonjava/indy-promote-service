package org.commonjava.service.promote.client;

import io.quarkus.test.junit.QuarkusTest;

import org.commonjava.service.promote.client.storage.FileCopyRequest;
import org.commonjava.service.promote.client.storage.FileCopyResult;
import org.commonjava.service.promote.client.storage.StorageService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@QuarkusTest
public class StorageServiceTest
{
    @Inject
    @RestClient
    StorageService storageService;

    @Test
    public void testCopy()
    {
        FileCopyRequest request = new FileCopyRequest();
        request.setSourceFilesystem( "source" );
        request.setTargetFilesystem( "target" );
        Set<String> paths = new HashSet<>();
        paths.add( "foo/bar/1.0/bar-1.0.jar" );
        request.setPaths( paths );
        Response response = storageService.copy( request );
        FileCopyResult ret = response.readEntity(FileCopyResult.class);
        Assertions.assertNotNull( ret );
        Assertions.assertTrue( ret.isSuccess() );
        Assertions.assertNull( ret.getMessage() );
    }
}
