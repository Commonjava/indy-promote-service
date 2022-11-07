package org.commonjava.service.promote.fixture;

import io.quarkus.test.Mock;
import org.apache.http.HttpStatus;
import org.commonjava.service.promote.client.storage.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;

@Mock
@RestClient
public class MockStorageService implements StorageService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String mockedStorageRootDir = "/tmp/storage";

    @Override
    public Response delete(String filesystem, String path) {
        logger.info( "Invoke storage delete, filesystem: {},path: {}", filesystem, path );
        String filesystemDir = filesystem.replaceAll(":", "/");
        File file = Paths.get(mockedStorageRootDir, filesystemDir, path ).toFile();
        file.delete();
        return Response.status(HttpStatus.SC_OK).build();
    }

    @Override
    public Response delete(BatchDeleteRequest request) {
        logger.info( "Invoke batch delete, request: {}", request );
        String filesystem = request.getFilesystem();
        if ( request.getPaths() != null )
        {
            request.getPaths().forEach( p -> delete( filesystem, p ));
        }
        BatchDeleteResult result = new BatchDeleteResult();
        result.setFilesystem( filesystem );
        result.setSucceeded( request.getPaths() );
        return Response.status(SC_OK).entity(result).build();
    }

    @Override
    public Response retrieve(String filesystem, String path) {
        String filesystemDir = filesystem.replaceAll(":", "/");
        File file = Paths.get(mockedStorageRootDir, filesystemDir, path ).toFile();
        //logger.debug("Retrieve file: {}, exist: {}", file.getAbsoluteFile(), file.isFile());
        try {
            return Response.status( SC_OK ).entity(new FileInputStream(file)).build();
        } catch (FileNotFoundException e) {
            return Response.status(SC_NOT_FOUND).build();
        }
    }

    @Override
    public Response put(String filesystem, String path, InputStream in) {
        String filesystemDir = filesystem.replaceAll(":", "/");
        File file = Paths.get(mockedStorageRootDir, filesystemDir, path ).toFile();
        File parent = file.getParentFile();
        if ( !parent.isDirectory() )
        {
            parent.mkdirs();
        }
        try {
            in.transferTo( new FileOutputStream( file ));
            return Response.status( SC_OK ).build();
        } catch (IOException e) {
            return Response.status(SC_INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response copy(FileCopyRequest request) {
        logger.info( "Invoke storage copy, request: {}", request );
        Set<String> paths = request.getPaths();
        Set<String> completed = new HashSet<>();
        Set<String> skipped = new HashSet<>();
        if ( paths != null )
        {
            if ( request.isFailWhenExists() )
            {
                // check them
                for (String path : paths) {
                    boolean targetExist = isTargetFileExist( request.getTargetFilesystem(), path );
                    if ( targetExist )
                    {
                        FileCopyResult result = new FileCopyResult(false);
                        result.setMessage("File already exists: " + path );
                        return Response.status(SC_OK).entity(result).build();
                    }
                }
            }
            for (String path : paths) {
                boolean targetExist = isTargetFileExist( request.getTargetFilesystem(), path );
                if ( targetExist )
                {
                    skipped.add( path );
                }
                else {
                    try {
                        copyFile(request.getSourceFilesystem(), request.getTargetFilesystem(), path);
                    } catch (IOException e) {
                        FileCopyResult result = new FileCopyResult(false);
                        e.printStackTrace();
                        result.setMessage("File copy failed: " + e.getMessage());
                        return Response.status(SC_OK).entity(result).build();
                    }
                    completed.add(path);
                }
            }
        }
        return Response.status(SC_OK).entity(new FileCopyResult( true, completed, skipped )).build();
    }

    private boolean isTargetFileExist(String targetFilesystem, String path)
    {
        String targetDir = targetFilesystem.replaceAll(":", "/");
        File targetFile = Paths.get(mockedStorageRootDir, targetDir, path ).toFile();
        return targetFile.exists();
    }

    private void copyFile(String sourceFilesystem, String targetFilesystem, String path)
        throws IOException
    {
        String sourceDir = sourceFilesystem.replaceAll(":", "/");
        String targetDir = targetFilesystem.replaceAll(":", "/");
        File sourceFile = Paths.get(mockedStorageRootDir, sourceDir, path ).toFile();
        File targetFile = Paths.get(mockedStorageRootDir, targetDir, path ).toFile();
        File parent = targetFile.getParentFile();
        if ( !parent.isDirectory() )
        {
            parent.mkdirs();
        }
        Files.copy(sourceFile.toPath(), targetFile.toPath());
    }

    @Override
    public Response exists(String filesystem, String path) {
        String filesystemDir = filesystem.replaceAll(":", "/");
        File file = Paths.get(mockedStorageRootDir, filesystemDir, path ).toFile();
        if (file.isFile()) {
            return Response.status( SC_OK ).build();
        }
        return Response.status(SC_NOT_FOUND).build();
    }

    @Override
    public Response list(String rawPath, boolean recursive, String fileType, int limit) {
        // rawPath is as /filesystem/path/...
        // In test, we only care about listing the whole filesystem
        if ( rawPath.startsWith("/"))
        {
            rawPath = rawPath.substring(1);
        }

        String[] tokens = rawPath.split("/", 2);
        String filesystem = tokens[0];
        String filesystemDir = filesystem.replaceAll(":", "/");

        File listDir = Paths.get(mockedStorageRootDir, filesystemDir ).toFile();

        int beginIndex = listDir.getAbsolutePath().length();
        List<String> paths;
        try (Stream<Path> stream = Files.walk(Paths.get(listDir.getAbsolutePath()))) {
            paths = stream.filter(Files::isRegularFile).map(path -> path.toString().substring(beginIndex)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] array = paths.toArray(new String[0]);
        return Response.ok(array).build();
    }

}

