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

import groovy.lang.Closure;
import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.config.TestPromoteConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromotionValidationToolsTest
{
    final String[] array = { "this", "is", "a", "err_weird", "test", "err_for", "paralleled", "err_in", "batch" };

    private PromoteConfig config = new TestPromoteConfig( null );

    @Test
    public void testParalleledInBatch()
    {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        PromotionValidationTools tools =
                        new PromotionValidationTools( null, executor, config );

        List<String> errors = Collections.synchronizedList( new ArrayList<>() );
        Closure closure = new Closure<String>( null )
        {
            @Override
            public String call( Object arg )
            {
                if ( ( (String) arg ).startsWith( "err_" ) )
                {
                    errors.add( (String) arg );
                }
                return null;
            }
        };

        tools.paralleledInBatch( array, closure );
        verifyIt( errors );
    }

    @Test
    public void testParalleledInBatch_smallSize()
    {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( 2 );
        PromotionValidationTools tools =
                        new PromotionValidationTools( null, executor, config );

        List<String> errors = Collections.synchronizedList( new ArrayList<>() );
        Closure closure = new Closure<String>( null )
        {
            @Override
            public String call( Object arg )
            {
                if ( ( (String) arg ).startsWith( "err_" ) )
                {
                    errors.add( (String) arg );
                }
                return null;
            }
        };

        tools.paralleledInBatch( array, closure );
        verifyIt( errors );
    }

    private void verifyIt( List<String> errors )
    {
        assertThat( errors.size(), equalTo( 3 ) );
        assertTrue( errors.contains( "err_weird" ) );
        assertTrue( errors.contains( "err_for" ) );
        assertTrue( errors.contains( "err_in" ) );
    }
}
