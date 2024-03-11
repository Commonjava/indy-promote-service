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

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CustomClientRequestFilter implements ClientRequestFilter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    OidcClient client;

    private volatile Tokens tokens;

    @ConfigProperty(name = "indy_security.enabled")
    boolean securityEnabled;

    private static final List<String> nonAuthMethods = Arrays.asList("GET", "HEAD"); // skip auth for GET/HEAD requests

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException
    {
        if ( securityEnabled )
        {
            String method = requestContext.getMethod().toUpperCase();
            if ( nonAuthMethods.contains(method) )
            {
                return;
            }
            if ( tokens == null || tokens.isAccessTokenExpired() )
            {
                logger.debug("Security enabled, get oidc Tokens");
                tokens = client.getTokens().await().indefinitely();
            }
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
        }
    }
}
