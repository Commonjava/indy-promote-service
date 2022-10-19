package org.commonjava.service.promote.handler;

import io.quarkus.test.Mock;
import io.restassured.internal.util.IOUtils;
import org.apache.http.HttpStatus;
import org.commonjava.service.promote.client.storage.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.http.HttpStatus.*;

@Mock
@RestClient
public class MockStorageService implements StorageService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String storageRootDir = "/tmp";

    @Override
    public Response delete(String filesystem, String path) {
        logger.info( "Invoke storage delete, filesystem: {},path: {}", filesystem, path );
        return Response.status(HttpStatus.SC_OK).build();
    }

    @Override
    public Response delete(BatchDeleteRequest request) {
        logger.info( "Invoke batch delete, request: {}", request );
        BatchDeleteResult result = new BatchDeleteResult();
        result.setFilesystem( request.getFilesystem() );
        result.setSucceeded( request.getPaths() );
        return Response.status(SC_OK).entity(result).build();
    }

    @Override
    public Response retrieve(String filesystem, String path) {
        File file = Paths.get( storageRootDir, filesystem, path ).toFile();
        try {
            return Response.status( SC_OK ).entity(IOUtils.toByteArray( new FileInputStream(file) )).build();
        } catch (FileNotFoundException e) {
            return Response.status(SC_NOT_FOUND).build();
        } catch (IOException e) {
            return Response.status(SC_INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response copy(FileCopyRequest request) {
        logger.info( "Invoke storage copy, request: {}", request );
        return Response.status(SC_OK).entity(new FileCopyResult( true )).build();
    }

    @Override
    public Response exists(String filesystem, String path) {
        File file = Paths.get( storageRootDir, filesystem, path ).toFile();
        if (file.isFile()) {
            return Response.status( SC_OK ).build();
        }
        return Response.status(SC_NOT_FOUND).build();
    }

}

