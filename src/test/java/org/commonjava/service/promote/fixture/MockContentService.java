package org.commonjava.service.promote.fixture;

import io.quarkus.test.Mock;
import io.quarkus.test.junit.mockito.InjectMock;
import org.commonjava.service.promote.client.content.ContentService;
import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.core.Response;

@Mock
@RestClient
public class MockContentService implements ContentService
{
    StorageService storageService = new MockStorageService();

    @Override
    public Response retrieve(String packageName, String type, String name, String path)
    {
        if ( type.equals(StoreType.hosted.getName()))
        {
            return storageService.retrieve( new StoreKey(packageName, StoreType.get(type), name).toString(), path);
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
