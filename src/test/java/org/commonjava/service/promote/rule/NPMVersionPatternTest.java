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

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.service.promote.TestHelper;
import org.commonjava.service.promote.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is used to test npm-version-pattern rule can work correctly for version checking:
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Rule-set with specified valid version pattern</li>
 *     <li>Source and target hosted repo with npm pkg type, and target repo matches rule-set store pattern</li>
 *     <li>Source repo contains valid versioned artifacts and invalid ones</li>
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
 *     <li>Validation errors only contains the paths for invalid versioned artifacts</li>
 * </ul>
 */
@QuarkusTest
public class NPMVersionPatternTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "npm", StoreType.hosted, "build-verptn" );

    private final StoreKey target = new StoreKey( "npm", StoreType.hosted, "test-verptn-builds" );;

    private static final String RULE = "npm-version-pattern";

    private static final String
            INVALID_PKG = "invalid",
            VALID_PKG = "valid",
            SCOPED_INVALID_PKG = "@scoped/invalid",
            SCOPED_VALID_PKG = "@scoped/valid",
            REDHAT_SCOPED_INVALID_PKG = "@redhat/invalid",
            REDHAT_SCOPED_VALID_PKG = "@redhat/valid",
            INVALID_TAR = "invalid/-/invalid-1.tgz",
            VALID_TAR = "valid/-/valid-1.5.tgz",
            SCOPED_INVALID_TAR = "@scoped/invalid/-/invalid-1.tgz",
            SCOPED_VALID_TAR = "@scoped/valid/-/valid-1.5.tgz",
            REDHAT_SCOPED_INVALID_TAR = "@redhat/invalid/-/invalid-1.tgz",
            REDHAT_SCOPED_VALID_TAR = "@redhat/valid/-/valid-1.5.tgz";

    private final String tarContent = "This is a test";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployResource(source, INVALID_PKG + "/package.json", "npm-version-pattern/package-invalid.json" );
        ruleTestHelper.deployResource(source, VALID_PKG + "/package.json", "npm-version-pattern/package-valid.json" );
        ruleTestHelper.deployResource(source, SCOPED_INVALID_PKG + "/package.json", "npm-version-pattern/package-scoped-invalid.json" );
        ruleTestHelper.deployResource(source, SCOPED_VALID_PKG + "/package.json", "npm-version-pattern/package-scoped-valid.json" );
        ruleTestHelper.deployResource(source, REDHAT_SCOPED_INVALID_PKG + "/package.json", "npm-version-pattern/package-redhat-scoped-invalid.json" );
        ruleTestHelper.deployResource(source, REDHAT_SCOPED_VALID_PKG + "/package.json", "npm-version-pattern/package-redhat-scoped-valid.json" );

        ruleTestHelper.deployContent(source, SCOPED_INVALID_TAR, tarContent);
        ruleTestHelper.deployContent(source, SCOPED_VALID_TAR, tarContent);
        ruleTestHelper.deployContent(source, INVALID_TAR, tarContent);
        ruleTestHelper.deployContent(source, VALID_TAR, tarContent);
        ruleTestHelper.deployContent(source, REDHAT_SCOPED_INVALID_TAR, tarContent);
        ruleTestHelper.deployContent(source, REDHAT_SCOPED_VALID_TAR, tarContent);
    }

    @Test
    public void run()
            throws Exception
    {
        PathsPromoteRequest request = new PathsPromoteRequest( source, target,
                INVALID_TAR,
                VALID_TAR,
                SCOPED_INVALID_TAR,
                SCOPED_VALID_TAR,
                REDHAT_SCOPED_INVALID_TAR,
                REDHAT_SCOPED_VALID_TAR );

        promoteAndCheck( request );

        request = new PathsPromoteRequest( source, target ); // promote package files
        promoteAndCheck( request );
    }

    private void promoteAndCheck(PathsPromoteRequest request )
            throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(request);

        ValidationResult validations = result.getValidations();
        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println( "Validation errors >>>\n" + validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        assertThat( errors.contains( VALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( INVALID_TAR ), equalTo( true ) );
        assertThat( errors.contains( SCOPED_VALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( SCOPED_INVALID_TAR ), equalTo( true ) );
        assertThat( errors.contains( REDHAT_SCOPED_INVALID_TAR ), equalTo( false ) );
        assertThat( errors.contains( REDHAT_SCOPED_VALID_TAR ), equalTo( false ) );
    }

}
