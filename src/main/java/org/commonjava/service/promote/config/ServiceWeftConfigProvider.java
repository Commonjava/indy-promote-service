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

import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import static org.commonjava.service.promote.config.ServiceWeftConfig.*;

@ApplicationScoped
public class ServiceWeftConfigProvider
{
    @Inject
    ServiceWeftConfig serviceWeftConfig;

    @Produces
    @Default
    public DefaultWeftConfig getWeftConfig()
    {
        DefaultWeftConfig ret = new DefaultWeftConfig();
        ret.configureThreads( PROMOTE_RUNNER, serviceWeftConfig.promoteRunner() );
        ret.configureThreads( PROMOTE_RULES_RUNNER, serviceWeftConfig.promoteRulesRunner() );
        ret.configureThreads( PROMOTE_RULES_BATCH_EXECUTOR, serviceWeftConfig.promoteRulesBatchExecutor() );
        return ret;
    }

}
