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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * For the target 'shared-imports', validation should fail when 'redhat' is in the path.
 */
@QuarkusTest
public class ProjectVersionPatternForSharedImportsTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "third-party" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "shared-imports" );;

    private static final String RULE = "project-version-pattern";

    final String valid = "org/foo/bar/8.0.0.GA-20230830/bar-8.0.0.GA-20230830.jar";

    final String invalid = "org/foo/bar/8.0.0.GA-redhat-20230830/bar-8.0.0.GA-redhat-20230830.jar";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent(source, invalid, "This is a test" );
        ruleTestHelper.deployContent(source, valid, "This is a test" );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(new PathsPromoteRequest(source, target));

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        //System.out.println( "Validation error:\n" + validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );
        assertThat( errors.contains( valid ), equalTo( false ) );
        assertThat( errors.contains( invalid ), equalTo( true ) );
    }

}
