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

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This tests npm-parsable-package-meta rule for parsable package.json.
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Rule-set with npm-parsable-package-meta in it</li>
 *     <li>Source repo contains valid and invalid package.json</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Do paths promotion from source repo to target repo</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Validation failed and validation errors for rule is in result</li>
 *     <li>Validation errors only contains the paths for invalid package.json</li>
 * </ul>
 */
@QuarkusTest
public class NPMParsablePkgMetaTest
{
    @Inject
    RuleTestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "npm", StoreType.hosted, "build-pkg" );

    private final StoreKey target = new StoreKey( "npm", StoreType.hosted, "pnc-builds" );;

    private static final String RULE = "npm-parsable-package-meta";

    private static final String INVALID = "wrong/package.json";
    private static final String VALID = "valid/package.json";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent(source, INVALID, "This is not parsable" );
        ruleTestHelper.deployResource(source, VALID, "npm-parsable-pkg-meta/package.json" );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteRequest request = new PathsPromoteRequest( source, target, VALID, INVALID );
        promoteAndCheck(request);

        request = new PathsPromoteRequest( source, target ); // do it again without explicitly specify paths
        promoteAndCheck(request);
    }

    private void promoteAndCheck(PathsPromoteRequest request) throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(request);

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        //System.out.println( "Validation errors >>>\n" + validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );
        assertThat( errors.contains( VALID ), equalTo( false ) );
        assertThat( errors.contains( INVALID ), equalTo( true ) );
    }

}
