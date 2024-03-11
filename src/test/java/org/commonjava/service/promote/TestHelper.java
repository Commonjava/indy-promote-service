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
package org.commonjava.service.promote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.util.DefaultPathGenerator;
import org.commonjava.service.promote.client.storage.StorageService;
import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.model.*;
import org.commonjava.service.promote.tracking.cassandra.DtxPromoteQueryByPath;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.commonjava.service.promote.PromoteResourceTest.PROMOTE_PATH;
import static org.commonjava.service.promote.PromoteResourceTest.ROLLBACK_PATH;
import static org.commonjava.service.promote.jaxrs.PromoteAdminResource.PROMOTION_ADMIN_API;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ApplicationScoped
public class TestHelper
{
    public final ObjectMapper mapper = new IndyObjectMapper();

    public static final String VALID_POM_EXAMPLE = "<?xml version=\"1.0\"?>\n" +
            "<project>" +
            "  <modelVersion>4.0.0</modelVersion>" +
            "  <groupId>org.foo</groupId>" +
            "  <artifactId>valid</artifactId>" +
            "  <version>1</version>" +
            "</project>";

    @Inject
    @RestClient
    StorageService storageService;

    private DefaultPathGenerator pathGenerator = new DefaultPathGenerator();

    public void deployResource(StoreKey repo, String path, String resourcePath) throws IOException
    {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream( resourcePath ))
        {
            storageService.put(repo.toString(), path, stream);
        }
    }

    public void deployContent(StoreKey repo, String path, String content)
    {
        storageService.put(repo.toString(), path, new ByteArrayInputStream(content.getBytes()));
    }

    public void deployContent(StoreKey repo, String path, PathStyle pathStyle, String content)
    {
        deployContent(repo, pathGenerator.getStyledPath(path, pathStyle), content);
    }

    public boolean exists(StoreKey repo, String path)
    {
        return storageService.exists(repo.toString(), path).getStatus() == SC_OK;
    }

    public PathsPromoteResult doPromote(final PathsPromoteRequest request) throws Exception
    {
        return doPromote( mapper.writeValueAsString(request) );
    }

    public PathsPromoteResult doPromote(final String requestJson) throws Exception
    {
        Response response =
                given().when()
                        .body(requestJson)
                        .header("Content-Type", APPLICATION_JSON)
                        .post(PROMOTE_PATH);

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        PathsPromoteResult result = mapper.readValue( content, PathsPromoteResult.class );
        assertNotNull( result );

        PathsPromoteRequest request = mapper.readValue(requestJson, PathsPromoteRequest.class);
        assertThat(result.getRequest().getSource(), equalTo(request.getSource()));
        assertThat(result.getRequest().getTarget(), equalTo(request.getTarget()));
        return result;
    }

    public PathsPromoteResult doRollback(final PathsPromoteResult result) throws Exception
    {
        Response response = given().when()
                .body(mapper.writeValueAsString(result))
                .header("Content-Type", APPLICATION_JSON)
                .post(ROLLBACK_PATH);

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        return mapper.readValue( content, PathsPromoteResult.class );
    }

    public PromoteTrackingRecords getTrackingRecords(final String trackingId) throws Exception
    {
        Response response = given().when()
                .get(PROMOTION_ADMIN_API + "/tracking/" + trackingId);

        if ( response.statusCode() == 404 )
        {
            return null;
        }

        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        return mapper.readValue( content, PromoteTrackingRecords.class );
    }

    public PromoteQueryByPath queryByPath(final StoreKey repo, final String path) throws Exception
    {
        Response response = given().when()
                .get(PROMOTION_ADMIN_API + "/query/" +
                        Paths.get(repo.getPackageType(), repo.getType().getName(), repo.getName(), path));

        if ( response.statusCode() == 404 )
        {
            return null;
        }

        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        return mapper.readValue( content, DtxPromoteQueryByPath.class );
    }

    public void doRecordsDeletion(String trackingId)
    {
        given().when().delete(PROMOTION_ADMIN_API + "/tracking/" + trackingId);
    }
}
