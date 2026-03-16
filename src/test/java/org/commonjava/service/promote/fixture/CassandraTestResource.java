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
package org.commonjava.service.promote.fixture;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class CassandraTestResource implements QuarkusTestResourceLifecycleManager
{
    private static final CassandraContainer<?> cassandraContainer =
        new CassandraContainer<>(DockerImageName.parse("cassandra:3.11.2"))
            .withInitScript("cql/init-keyspace.cql");

    @Override
    public Map<String, String> start()
    {
        cassandraContainer.start();

        Map<String, String> config = new HashMap<>();
        config.put("cassandra.host", cassandraContainer.getHost());
        config.put("cassandra.port", String.valueOf(cassandraContainer.getMappedPort(9042)));
        config.put("cassandra.enabled", "true");
        config.put("cassandra.keyspace", "promote_tracking");

        return config;
    }

    @Override
    public void stop()
    {
        if (cassandraContainer != null)
        {
            cassandraContainer.stop();
        }
    }
}
