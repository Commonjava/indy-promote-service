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
package org.commonjava.service.promote.client;

import io.quarkus.test.junit.QuarkusTest;

import org.commonjava.service.promote.client.storage.FileCopyRequest;
import org.commonjava.service.promote.client.storage.FileCopyResult;
import org.commonjava.service.promote.client.storage.StorageService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
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
        String path = "foo/bar/1.0/bar-1.0.jar";
        storageService.put( "source", path, new ByteArrayInputStream( "This is a test".getBytes() ));

        FileCopyRequest request = new FileCopyRequest();
        request.setSourceFilesystem( "source" );
        request.setTargetFilesystem( "target" );
        Set<String> paths = new HashSet<>();
        paths.add( path );
        request.setPaths( paths );
        Response response = storageService.copy( request );
        FileCopyResult ret = response.readEntity(FileCopyResult.class);
        //System.out.println(">>>" + ret);
        Assertions.assertNotNull( ret );
        Assertions.assertTrue( ret.isSuccess() );
        Assertions.assertNull( ret.getMessage() );
    }
}
