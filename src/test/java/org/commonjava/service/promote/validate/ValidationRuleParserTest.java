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
package org.commonjava.service.promote.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.service.promote.model.ValidationRuleSet;
import org.commonjava.service.promote.util.ScriptEngine;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ValidationRuleParserTest
{
    private ValidationRuleParser parser = new ValidationRuleParser( new ScriptEngine(), new ObjectMapper() );

    @Test
    public void testRuleSetParseAndMatchOnStoreKey()
            throws Exception
    {
        ValidationRuleSet ruleSet =
                parser.parseRuleSet( "{\"name\":\"test\",\"storeKeyPattern\":\".*\"}", "test.json" );

        assertThat( ruleSet, notNullValue() );
        assertThat( ruleSet.matchesKey( "hosted:repo" ), equalTo( true ) );
    }
}
