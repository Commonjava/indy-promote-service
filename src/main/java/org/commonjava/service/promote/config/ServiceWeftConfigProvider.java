package org.commonjava.service.promote.config;

import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

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
