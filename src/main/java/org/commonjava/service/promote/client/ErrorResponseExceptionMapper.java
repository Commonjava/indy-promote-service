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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ErrorResponseExceptionMapper
        implements ResponseExceptionMapper<RuntimeException>
{

    /**
     * If toThrowable returns an exception, then that exception will be thrown. If null is returned, the next
     * implementation of ResponseExceptionMapper in the chain will be called (if there is any).
     */
    @Override
    public RuntimeException toThrowable(Response response)
    {
        if (response.getStatus() == 500)
        {
            Object entity = response.getEntity();
            if (entity != null)
            {
                if (entity instanceof InputStream)
                {
                    try (InputStream is = (InputStream) entity)
                    {
                        throw new WebApplicationException(IOUtils.toString(is, Charset.defaultCharset()));
                    }
                    catch (IOException e)
                    {
                        throw new WebApplicationException("Unknown error, " + e.getMessage());
                    }
                }
                else
                {
                    throw new WebApplicationException(entity.toString());
                }
            }
            throw new WebApplicationException("Unknown error");
        }
        return null;
    }
}