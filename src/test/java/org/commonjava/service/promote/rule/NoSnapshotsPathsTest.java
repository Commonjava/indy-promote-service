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

@QuarkusTest
public class NoSnapshotsPathsTest
{
    @Inject
    TestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-nsd" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    private static final String RULE = "no-snapshot-paths";

    String snapshot = "org/foo/valid/1.1.1-SNAPSHOT/valid-1.1.1-SNAPSHOT.pom";
    String valid = "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.pom";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent(source, snapshot, "This is a test" );
        ruleTestHelper.deployContent(source, valid, "This is a test" );
    }

    @Test
    public void run()
            throws Exception
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
        assertThat( errors.contains(snapshot), equalTo( true ) );
    }

}
