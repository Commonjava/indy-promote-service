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
package org.commonjava.service.promote.util;

import groovy.lang.GroovyClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import java.util.Arrays;

@ApplicationScoped
public class ScriptEngine
{
    private final Logger logger = LoggerFactory.getLogger( ScriptEngine.class.getName() );

    private final GroovyClassLoader groovyClassloader = new GroovyClassLoader();

    public ScriptEngine()
    {
    }

    public <T> T parseScriptInstance( final String script, final Class<T> type )
            throws Exception
    {
        return parseScriptInstance( script, type, false );
    }

    public <T> T parseScriptInstance( final String script, final Class<T> type, boolean processCdiInjections )
            throws Exception
    {
        final Class<?> clazz = groovyClassloader.parseClass( script );
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Parsed: {} (type: {}, interfaces: {})", instance, instance.getClass(),
                     Arrays.asList( instance.getClass().getInterfaces() ) );

        T result = type.cast( instance );
        return processCdiInjections ? inject( result ) : result ;
    }

    // Scripts that can use CDI injection will need to use this method to inject their fields.
    @Inject
    BeanManager beanManager;

    private <T> T inject( T bean )
    {
        CreationalContext<T> ctx = beanManager.createCreationalContext( null );

        AnnotatedType<T> annotatedType =
                beanManager.createAnnotatedType( (Class<T>) bean.getClass() );

        InjectionTarget<T> injectionTarget =
                beanManager.createInjectionTarget( annotatedType );

        injectionTarget.inject( bean, ctx );

        return bean;
    }
}
