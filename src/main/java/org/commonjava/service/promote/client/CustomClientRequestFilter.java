package org.commonjava.service.promote.client;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
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
