package org.commonjava.service.promote.rule;

import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.model.StoreKey;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class RuleTestHelper {

    @Inject
    @RestClient
    StorageService storageService;

    void deployResource(StoreKey repo, String path, String resourcePath) throws IOException
    {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream( resourcePath ))
        {
            storageService.put(repo.toString(), path, stream);
        }
    }

}
