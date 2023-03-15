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
package org.commonjava.service.promote.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.fixture.TestResources;
import org.commonjava.service.promote.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * When <br />
 *
 *  <ol>
 *    <li>Two hosted repositories have same path deployed</li>
 *    <li>promotion validation rule-set that includes no-pre-existing-paths.groovy</li>
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
    TestHelper ruleTestHelper;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String RULE = "no-pre-existing-paths";

    private final String resourceDir = "no-pre-existing-paths/";

    private final StoreKey host1 = new StoreKey( "maven", StoreType.hosted, "build-1" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds-fullstack" );;

    private final List<String> paths = Arrays.asList(
            "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.pom",
            "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.jar",
            "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1-sources.jar",
            "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1-javadoc.jar" );

    @BeforeEach
    public void prepare()
    {
        // Deploy different files in src and target to make the 'no-pre-existing-paths' report errors
        paths.forEach( path -> {
            try {
                ruleTestHelper.deployResource( host1, path, resourceDir + "valid.pom.xml" );
                ruleTestHelper.deployResource( target, path, resourceDir + "invalid.pom.xml" );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote( new PathsPromoteRequest( host1, target ) );

        ValidationResult validations = result.getValidations();
        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        //System.out.println(">>>\n" + validatorErrors);
        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );
    }

}
