package org.commonjava.service.promote.core;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.util.ContentDigest;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_OK;

@ApplicationScoped
public class ContentDigester {

    @Inject
    @RestClient
    StorageService storageService;

    public ContentDigester() {
    }

    public String digest(StoreKey key, String path, ContentDigest digest) throws IOException {
        // retrieve the checksum file if exists
        Response resp = storageService.retrieve(key.toString(), path + digest.getFileExt());
        if ( resp.getStatus() == SC_OK )
        {
            String content = resp.readEntity(String.class);
            if ( isNotBlank( content ))
            {
                return content.trim();
            }
        }
        // retrieve the raw file and calculate checksum
        resp = storageService.retrieve(key.toString(), path);
        if ( resp.getStatus() == SC_OK )
        {
            try (InputStream is = resp.readEntity( InputStream.class ))
            {
                return DigestUtils.sha256Hex( is );
            }
        }
        return null;
    }
}
