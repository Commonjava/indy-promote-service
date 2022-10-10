package org.commonjava.service.promote.handler;

import io.quarkus.test.Mock;
import org.apache.http.HttpStatus;
import org.commonjava.service.promote.client.storage.FileCopyRequest;
import org.commonjava.service.promote.client.storage.FileCopyResult;
import org.commonjava.service.promote.client.storage.StorageService;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static org.apache.http.HttpStatus.SC_OK;

@Mock
@RestClient
public class MockStorageService implements StorageService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public Response delete(String filesystem, String path) {
        logger.info( "Invoke storage delete, filesystem: {},path: {}", filesystem, path );
        return Response.status(HttpStatus.SC_OK).build();
    }

    @Override
    public Response copy(FileCopyRequest request) {
        logger.info( "Invoke storage copy, request: {}", request );
        return Response.status(SC_OK).entity(new FileCopyResult( true )).build();
    }

}

