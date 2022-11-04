/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.service.promote.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.model.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.commonjava.service.promote.PromoteResourceTest.PROMOTE_PATH;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * When <br />
 *
 *  <ol>
 *    <li>Two hosted repositories have same path deployed</li>
 *    <li>promotion validation rule-set that includes no-pre-existing-paths.groovy and targets the group</li>
 *    <li>path promotion request posted</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *    <li>the no-pre-existing-paths.groovy rule should be triggered with validation error</li>
 *  </ol>
 *
 */
@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class MavenFullRuleStackTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @RestClient
    StorageService storageService;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String RULE = "no-pre-existing-paths";

    private static final String PREFIX = "no-pre-existing-paths/";

    /* @formatter:off */
    private static final String[] RULES = {
        "parsable-pom.groovy",
        "no-pre-existing-paths.groovy",
        "no-snapshots-paths.groovy",
        "project-version-pattern.groovy"
    };
    /* @formatter:on */

    private StoreKey host1 = new StoreKey( "maven", StoreType.hosted, "build-1" );

    private StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds-fullstack" );;

    @Test
    public void run() throws Exception
    {
        List<String> paths = Arrays.asList(
                "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.pom",
                "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.jar",
                "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1-sources.jar",
                "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1-javadoc.jar" );

        Stream.of( host1, target ).forEach( (repo)-> paths.forEach( path -> {
            try
            {
                deployResource( repo, path, PREFIX + "valid.pom.xml" );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                fail( "Failed to deploy: " + path + " to: " + repo );
            }
        } ));

        PathsPromoteRequest promoteRequest = new PathsPromoteRequest( host1, target );

        // Promote
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
        assertNotNull( result.getError() );

        ValidationResult validations = result.getValidations();
        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println(">>>\n" + validatorErrors);
        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() ); // TODO: fix this. storage file not in right place
        //assertThat( errors.contains( deploy ), equalTo( true ) );
    }

    private void deployResource(StoreKey repo, String path, String resourcePath) throws IOException
    {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream( resourcePath ))
        {
            storageService.put(repo.toString(), path, stream);
        }
    }

}
