package org.commonjava.service.promote.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.commonjava.service.promote.PromoteResourceTest.PROMOTE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ApplicationScoped
public class RuleTestHelper
{
    private ObjectMapper mapper = new ObjectMapper();

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

    void deployContent(StoreKey repo, String path, String content)
    {
        storageService.put(repo.toString(), path, new ByteArrayInputStream(content.getBytes()));
    }

    public PathsPromoteResult doPromote(PathsPromoteRequest promoteRequest) throws Exception
    {
        Response response =
                given().when()
                        .body(mapper.writeValueAsString(promoteRequest))
                        .header("Content-Type", APPLICATION_JSON)
                        .post(PROMOTE_PATH);

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        PathsPromoteResult result = mapper.readValue( content, PathsPromoteResult.class );
        assertNotNull( result );
        return result;
    }
}
