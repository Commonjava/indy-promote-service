package org.commonjava.service.promote.config;

import io.quarkus.runtime.Startup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import javax.enterprise.context.ApplicationScoped;

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
