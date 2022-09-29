package org.commonjava.service.promote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.response.Response;
import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.StoreType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.commonjava.service.promote.fixture.TestResources.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class PromoteResourceTest
{
    public static final String PROMOTE_PATH = "/api/promotion/paths/promote";

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPromote() throws JsonProcessingException {
        StoreKey src = new StoreKey( "maven", StoreType.hosted, "build-1" );
        StoreKey tgt = new StoreKey( "maven", StoreType.hosted, "test-builds" );
        Set<String> paths = new HashSet<>();
        paths.add( "foo/bar/1.0/bar-1.0.pom" );
        paths.add( "foo/bar/1.0/bar-1.0.jar" );
        PathsPromoteRequest promoteRequest = new PathsPromoteRequest( src, tgt, paths );

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
    }

}