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
import org.apache.commons.io.FileUtils;

import org.commonjava.service.promote.model.ValidationRuleSet;
import org.commonjava.service.promote.util.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.charset.Charset.defaultCharset;

@ApplicationScoped
public class ValidationRuleParser
{
    private final String STANDARD_IMPORTS = "import " + this.getClass().getPackageName() + ".*;";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScriptEngine scriptEngine;

    @Inject
    private ObjectMapper objectMapper;

    protected ValidationRuleParser()
    {
    }

    public ValidationRuleParser( final ScriptEngine scriptEngine, ObjectMapper objectMapper )
    {
        this.scriptEngine = scriptEngine;
        this.objectMapper = objectMapper;
    }

    public ValidationRuleMapping parseRule( final File script )
            throws Exception
    {
        String spec = null;
        try
        {
            spec = FileUtils.readFileToString( script, defaultCharset() );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Cannot load validation rule from: %s. Reason: %s", script, e.getMessage() ), e );
            throw e;
        }

        if ( !spec.contains( "import " ) && !spec.contains( "package " ) )
        {
            spec = STANDARD_IMPORTS + spec;
        }

        return parseRule( spec, script.getName() );
    }

    public ValidationRuleMapping parseRule( final String spec, final String scriptName )
            throws PromotionValidationException
    {
        if ( spec == null )
        {
            return null;
        }

        logger.trace( "Parsing rule: {}, content:\n{}", scriptName, spec );

        ValidationRule rule;
        try
        {

            rule = scriptEngine.parseScriptInstance( spec, ValidationRule.class );
            logger.debug( "Parsed: {}", rule.getClass().getName() );
        }
        catch ( final Exception e )
        {
            throw new PromotionValidationException(
                    "Cannot load validation rule from: {} as an instance of: {}. Reason: {}", e, scriptName,
                    ValidationRule.class.getSimpleName(), e.getMessage() );
        }

        if ( rule != null )
        {
            return new ValidationRuleMapping( scriptName, spec, rule );
        }

        return null;
    }

    public ValidationRuleSet parseRuleSet(File script )
            throws PromotionValidationException
    {
        String spec = null;
        try
        {
            spec = FileUtils.readFileToString( script, defaultCharset() );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Cannot load validation rule-set from: %s. Reason: %s", script,
                                         e.getMessage() ), e );
        }

        return parseRuleSet( spec, script.getName() );
    }

    private ValidationRuleSet parseRuleSet( final String spec, final String scriptName )
            throws PromotionValidationException
    {
        if ( spec == null )
        {
            return null;
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Parsing rule-set from: {} with content:\n{}\n", scriptName, spec );

        try
        {

            ValidationRuleSet rs = objectMapper.readValue( spec, ValidationRuleSet.class );
            rs.setName( scriptName );
            return rs;
        }
        catch ( final IOException e )
        {
            throw new PromotionValidationException(
                    "Cannot load validation rule-set from: {} as an instance of: {}. Reason: {}", e,
                    scriptName, ValidationRule.class.getSimpleName(), e.getMessage() );
        }
    }

}
