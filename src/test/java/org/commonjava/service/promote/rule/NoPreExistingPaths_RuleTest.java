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
import io.quarkus.test.junit.QuarkusTest;

import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * When <br />
 *
 *  <ol>
 *    <li>both source and target have a same path deployed</li>
 *    <li>source has one other path deployed</li>
 *    <li>promotion validation rule-set that includes no-pre-existing-paths.groovy</li>
 *    <li>path promotion request posted</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *    <li>the no-pre-existing-paths.groovy rule should be triggered with validation error that only has one error</li>
 *  </ol>
 *
 */
@QuarkusTest
public class NoPreExistingPaths_RuleTest
{
    @Inject
    TestHelper ruleTestHelper;

    private ObjectMapper mapper = new ObjectMapper();

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-r" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    private static final String RULE = "no-pre-existing-paths";

    private final String resourceDir = "no-pre-existing-paths/";

    private String invalid = "org/foo/invalid/1/invalid-1.pom";
    private String valid = "org/foo/valid/1.1/valid-1.1.pom";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent( target, invalid, "This is a test" );

        ruleTestHelper.deployResource( source, invalid, resourceDir + "invalid.pom.xml");
        ruleTestHelper.deployResource( source, valid, resourceDir + "valid.pom.xml" );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(
                new PathsPromoteRequest( source, target ).setPurgeSource( true ) );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        //System.out.println(validatorErrors);
        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );
        assertThat( errors.contains( valid ), equalTo( false ) );
        assertThat( errors.contains( invalid ), equalTo( true ) );
    }

}
