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
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class ParsablePomTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-parsepom" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    private static final String RULE = "parsable-pom";

    @Test
    public void run() throws Exception
    {
        String invalid = "org/foo/invalid/1/invalid-1.pom";
        String valid = "org/foo/valid/1/valid-1.pom";

        ruleTestHelper.deployContent(source, invalid, "This is not parsable" );
        ruleTestHelper.deployContent(source, valid, "<?xml version=\"1.0\"?>\n" +
                "<project>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <groupId>org.foo</groupId>" +
                "  <artifactId>valid</artifactId>" +
                "  <version>1</version>" +
                "</project>" );

        PathsPromoteRequest request = new PathsPromoteRequest( source, target );
        request.setPaths( new HashSet( Arrays.asList( invalid, valid )) );

        PathsPromoteResult result = ruleTestHelper.doPromote(request);

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        //System.out.println( validatorErrors );

        String errors = validatorErrors.get( RULE );
        assertThat( errors, notNullValue() );
        assertThat( errors.contains( valid ), equalTo( false ) );
        assertThat( errors.contains( invalid ), equalTo( true ) );
    }
}
