package org.commonjava.service.promote.fixture;

import io.quarkus.test.Mock;
import org.commonjava.service.promote.client.content.ContentService;
import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;

import static org.commonjava.service.promote.PromoteRemoteReDownloadTest.PATH_MISSING_BUT_CAN_BE_RE_DOWNLOAD;

@Mock
@RestClient
public class MockContentService implements ContentService
{
    StorageService storageService = new MockStorageService();

    @Override
    public Response retrieve(String packageName, String type, String name, String path)
    {
        StoreKey storeKey = new StoreKey(packageName, StoreType.get(type), name);
        if ( type.equals(StoreType.hosted.getName()))
        {
            return storageService.retrieve( storeKey.toString(), path);
        }
        if ( type.equals(StoreType.remote.getName()) && path.equals(PATH_MISSING_BUT_CAN_BE_RE_DOWNLOAD))
        {
            // this is used to mock re-download
            storageService.put( storeKey.toString(), PATH_MISSING_BUT_CAN_BE_RE_DOWNLOAD,
                    new ByteArrayInputStream( "This is a test".getBytes()));
            return Response.ok().build();
        }
        return null;
    }

    @Override
    public Response exists(String packageName, String type, String name, String path)
    {
        if ( type.equals(StoreType.hosted.getName()))
        {
            return storageService.exists( new StoreKey(packageName, StoreType.get(type), name).toString(), path);
        }
        return null;
    }
}
