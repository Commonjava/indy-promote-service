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
package org.commonjava.service.promote.config;

import io.quarkus.runtime.Startup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import jakarta.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
@ConfigMapping( prefix = "promote.threadpools" )
public interface ServiceWeftConfig
{
    String PROMOTE_RUNNER = "promote-runner";

    String PROMOTE_RULES_RUNNER = "promote-rules-runner";

    String PROMOTE_RULES_BATCH_EXECUTOR = "promote-rules-batch-executor";

    @WithName( PROMOTE_RUNNER )
    @WithDefault( "8" )
    int promoteRunner();

    @WithName( PROMOTE_RULES_RUNNER )
    @WithDefault( "16" )
    int promoteRulesRunner();

    @WithName( PROMOTE_RULES_BATCH_EXECUTOR )
    @WithDefault( "16" )
    int promoteRulesBatchExecutor();
}
