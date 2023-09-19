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

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.service.promote.client.content.ContentService;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.util.ContentDigest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_OK;

@ApplicationScoped
public class ContentDigester
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @RestClient
    ContentService contentService;

    public ContentDigester() {
    }

    public String digest(StoreKey key, String path, ContentDigest digest) throws Exception
    {
        // retrieve the checksum file if exists
        Response resp = contentService.retrieve(key.getPackageType(), key.getType().getName(), key.getName(), path + digest.getFileExt());
        if ( resp.getStatus() == SC_OK )
        {
            String content = resp.readEntity(String.class);
            if ( isNotBlank( content ))
            {
                logger.debug("Get checksum {}/{}{}, {}", key, path, digest.getFileExt(), content);
                return content.trim();
            }
        }
        // retrieve the raw file and calculate checksum
        resp = contentService.retrieve(key.getPackageType(), key.getType().getName(), key.getName(), path);
        if ( resp.getStatus() == SC_OK )
        {
            try (InputStream is = resp.readEntity( InputStream.class ))
            {
                String checksum = DigestUtils.sha256Hex( is );
                logger.debug("Retrieve and digest {}/{}, {}", key, path, checksum);
                return checksum;
            }
        }
        logger.debug("Digest failed, {}/{}, code: {}", key, path, resp.getStatus());
        return null;
    }
}
