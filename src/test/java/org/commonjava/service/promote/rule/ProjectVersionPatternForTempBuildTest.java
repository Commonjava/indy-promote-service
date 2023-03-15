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

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Validate version pattern rules for a complex pattern (of temporary build)
 * When <br />
 *
 *  <ol>
 *    <li>Hosted source and target group</li>
 *    <li>source contains bunch of jar, javadoc, source jars</li>
 *    <li>promotion validation rule-set that includes project-version-pattern.groovy, which predefine pattern
 *    to limit some of the artifacts in source</li>
 *    <li>by-group promotion request posted</li>
 *  </ol>
 *
 *  Then <br />
 *
 *  <ol>
 *    <li>promotion failed, and validation result contains errors for un-matched artifacts against pattern</li>
 *  </ol>
 *
 */
@QuarkusTest
public class ProjectVersionPatternForTempBuildTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-verptn" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-verptn-temp-builds" );;

    private static final String RULE = "project-version-pattern";

    private String invalidJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004.jar";
    private String validJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004.jar";
    private String invalidDocJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004-javadoc.jar";
    private String validDocJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004-javadoc.jar";
    private String invalidSrcJar = "org/foo/invalid/1.0.0.redhat-00004/invalid-1.0.0.redhat-00004-sources.jar";
    private String validSrcJar = "org/foo/valid/1.0.0.temporary-redhat-00004/valid-1.0.0.temporary-redhat-00004-sources.jar";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent(source, invalidJar, "just for test of invalid jar" );
        ruleTestHelper.deployContent(source, validJar, "just for test of valid jar" );
        ruleTestHelper.deployContent(source, invalidDocJar, "just for test of invalid javadoc jar" );
        ruleTestHelper.deployContent(source, validDocJar, "just for test of valid javadoc jar" );
        ruleTestHelper.deployContent(source, invalidSrcJar, "just for test of invalid source jar" );
        ruleTestHelper.deployContent(source, validSrcJar, "just for test of valid source jar" );
    }

    @Test
    public void run()
            throws Exception
    {
        PathsPromoteRequest request = new PathsPromoteRequest(source, target);
        PathsPromoteResult result = ruleTestHelper.doPromote(request);

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );
        assertThat(validations.isValid(), equalTo( false ));

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );
        assertThat( validatorErrors.size(), not( 0 ) );

        System.out.println( "Validation error:\n" + validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );

        assertThat( errors.contains( validJar ), equalTo( false ) );
        assertThat( errors.contains( invalidJar ), equalTo( true ) );
        assertThat( errors.contains( validDocJar ), equalTo( false ) );
        assertThat( errors.contains( invalidDocJar ), equalTo( true ) );
        assertThat( errors.contains( validSrcJar ), equalTo( false ) );
        assertThat( errors.contains( invalidSrcJar ), equalTo( true ) );
    }

}
