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
package org.commonjava.service.promote.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.service.promote.config.TestPromoteConfig;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.ValidationRuleSet;
import org.commonjava.service.promote.util.PromoteDataFileManager;
import org.commonjava.service.promote.util.ScriptEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PromoteValidationsManagerTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ValidationRuleParser parser;

    private PromoteDataFileManager promoteFileManager;

    private PromoteValidationsManager promoteValidations;

    private TestPromoteConfig config;

    @Before
    public void setUp()
            throws Exception
    {
        File base = temp.newFolder("data" );
        config = new TestPromoteConfig();
        config.setBaseDir( base );

        promoteFileManager = new PromoteDataFileManager( config );
        parser = new ValidationRuleParser( new ScriptEngine(), new ObjectMapper() );

    }

    @Test
    public void testRuleSetParseAndMatchOnStoreKey()
            throws Exception
    {
        temp.newFolder( "data", "rule-sets" );
        File dataFile = new File( config.baseDir(), "rule-sets/test.json" );
        String content = "{\"name\":\"test\",\"storeKeyPattern\":\".*\"}";
        IOUtils.write( content, new FileOutputStream( dataFile ), Charset.defaultCharset() );

        promoteValidations = new PromoteValidationsManager( promoteFileManager, config, parser );

        ValidationRuleSet ruleSet = promoteValidations.getRuleSetMatching( StoreKey.fromString( "maven:hosted:repo" ) );

        assertThat( ruleSet, notNullValue() );
        assertThat( ruleSet.matchesKey( "hosted:repo" ), equalTo( true ) );
    }
}
