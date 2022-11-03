package org.commonjava.service.promote.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.commonjava.service.promote.model.ValidationRuleDTO;
import org.commonjava.service.promote.model.ValidationRuleSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.commonjava.service.promote.jaxrs.PromoteAdminResource.PROMOTION_ADMIN_API;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate rules & ruleset get functions through REST endpoints
 * When <br />
 *
 *  <ol>
 *      <li>Deployed rules files and rule-sets files</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *      <li>Can get all rule & rule-set file names through REST endpoints</li>
 *      <li>Can get rule file content through REST endpoints</li>
 *      <li>Can get rule-set file content through REST endpoints</li>
 *  </ol>
 *
 */
@QuarkusTest
public class GetRuleAndRuleSetTest
{

    private ObjectMapper mapper = new ObjectMapper();

    private final String[] defaultRuleNames = {
            "npm-no-pre-existing-paths",
            "no-pre-existing-paths",
            "npm-parsable-package-meta",
            "parsable-pom",
            "npm-version-pattern",
            "no-snapshot-paths",
            "project-version-pattern" };

    private final String[] getDefaultRuleSetNames = {
            "npm-pnc-builds.json",
            "npm-temporary-builds.json",
            "maven-temporary-builds.json",
            "shared-imports.json",
            "maven-pnc-builds.json" };

    @Test
    public void testGetRules() throws Exception
    {
        Response response =
                given()
                    .when()
                    .get(PROMOTION_ADMIN_API + "/validation/rules/all");

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        List<String> result = mapper.readValue( content, ArrayList.class );
        assertNotNull( result );
        List<String> expected = Arrays.asList(defaultRuleNames);
        assertTrue( result.containsAll( expected ) );
    }

    @Test
    public void testGetRuleByName() throws Exception
    {
        String ruleName = "npm-no-pre-existing-paths";
        Response response =
                given()
                    .pathParam( "name", ruleName )
                    .when()
                    .get(PROMOTION_ADMIN_API + "/validation/rules/named/{name}");

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        ValidationRuleDTO result = mapper.readValue( content, ValidationRuleDTO.class );
        assertNotNull( result );
        assertTrue( result.getName().equals(ruleName) );
    }

    @Test
    public void testGetRuleSets() throws Exception
    {
        Response response =
                given()
                        .when()
                        .get(PROMOTION_ADMIN_API + "/validation/rulesets/all");

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        List<String> result = mapper.readValue( content, ArrayList.class );
        assertNotNull( result );
        List<String> expected = Arrays.asList(getDefaultRuleSetNames);
        assertTrue( result.containsAll( expected ) );
    }

    @Test
    public void testGetRuleSetByStoreKey() throws Exception
    {
        String storeKey = "maven:hosted:pnc-builds";
        Response response =
                given()
                        .pathParam( "storeKey", storeKey )
                        .when()
                        .get(PROMOTION_ADMIN_API + "/validation/rulesets/storekey/{storeKey}" );

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        ValidationRuleSet result = mapper.readValue( content, ValidationRuleSet.class );
        assertNotNull( result );
        assertTrue( result.getName().equals("maven-pnc-builds.json") );
    }

    @Test
    public void testGetRuleSetByName() throws Exception
    {
        String name = "maven-pnc-builds.json";
        Response response =
                given()
                        .pathParam( "name", name )
                        .when()
                        .get(PROMOTION_ADMIN_API + "/validation/rulesets/named/{name}" );

        assertEquals( 200, response.statusCode() );
        String content = response.getBody().asString();
        //System.out.println(">>>\n" + content);
        ValidationRuleSet result = mapper.readValue( content, ValidationRuleSet.class );
        assertNotNull( result );
        assertTrue( result.getName().equals(name) );
    }
}
