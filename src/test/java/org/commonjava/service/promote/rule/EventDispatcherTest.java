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

/**
 * This is a normal success case. The purpose is to ensure KafkaEventDispatcher send() returns until
 * MockPromoteEventConsumer finishes handling the event by checking the log.
 */
@QuarkusTest
public class EventDispatcherTest
{
    @Inject
    RuleTestHelper ruleTestHelper;

    private final StoreKey source = new StoreKey( "maven", StoreType.hosted, "build-evt" );

    private final StoreKey target = new StoreKey( "maven", StoreType.hosted, "test-builds" );;

    String valid = "org/foo/valid/1.1.0.Final-redhat-1/valid-1.1.0.Final-redhat-1.pom";

    @BeforeEach
    public void prepare() throws IOException
    {
        ruleTestHelper.deployContent(source, valid, "This is a test" );
    }

    @Test
    public void run()
            throws Exception
    {
        PathsPromoteResult result = ruleTestHelper.doPromote(
                new PathsPromoteRequest( source, target ).setPurgeSource( true ) );
    }

}
