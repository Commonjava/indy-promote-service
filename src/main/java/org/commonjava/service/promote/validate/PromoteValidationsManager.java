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

import io.quarkus.runtime.Startup;
import org.apache.commons.io.IOUtils;
import org.commonjava.service.promote.config.PromoteConfig;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.ValidationCatalogDTO;
import org.commonjava.service.promote.model.ValidationRuleDTO;
import org.commonjava.service.promote.model.ValidationRuleSet;
import org.commonjava.service.promote.util.PromoteDataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

@Startup
@ApplicationScoped
public class PromoteValidationsManager
{

    public static final String RULES_DIR = "rules";

    public static final String RULES_SETS_DIR = "rule-sets";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    PromoteDataFileManager dataFileManager;

    @Inject
    PromoteConfig config;

    @Inject
    ValidationRuleParser ruleParser;

    private Map<String, ValidationRuleMapping> ruleMappings = new HashMap<>();

    private boolean enabled;

    private Map<String, ValidationRuleSet> ruleSets = new HashMap<>();

    protected PromoteValidationsManager()
    {
    }

    public PromoteValidationsManager( final PromoteDataFileManager dataFileManager, final PromoteConfig config,
                                      final ValidationRuleParser ruleParser )
        throws Exception
    {
        this.dataFileManager = dataFileManager;
        this.config = config;
        this.ruleParser = ruleParser;
        parseRuleBundles();
    }

    @PostConstruct
    public void cdiInit()
    {
        try
        {
            parseRuleBundles();
        }
        catch ( final Exception e )
        {
            logger.error( "Failed to parse validation rule: " + e.getMessage(), e );
        }
    }

    private void parseRuleBundles() throws Exception
    {
        parseRules();
        parseRuleSets();

        this.enabled = true;
    }

    private void parseRules() throws Exception {
        // Load default rules
        logger.info("Load default validation rules from resource.");
        List<String> defaultRules = getResourceRuleFiles( RULES_DIR );
        for (final String f : defaultRules) {
            logger.info("Load default validation rule: {}", f );
            String script = getResourceRuleAsString( f );
            final ValidationRuleMapping rule = ruleParser.parseRule(script, normalizeRuleName(f));
            if (rule != null) {
                ruleMappings.put(rule.getName(), rule);
            }
        }

        // Load customer rules
        File rulesDir = dataFileManager.getDataFile(RULES_DIR);
        logger.info("Scanning {} for validation rules...", rulesDir);
        if (rulesDir.exists()) {
            logger.info("Load rules in directory: {}", rulesDir);
            File[] scripts = rulesDir.listFiles((f) -> f.getName().endsWith(".groovy"));

            if (scripts.length > 0) {
                for (final File script : scripts) {
                    logger.info("Load validation rule from: {}", script);
                    final ValidationRuleMapping rule = ruleParser.parseRule(script, normalizeRuleName(script.getName()));
                    if (rule != null) {
                        ruleMappings.put(rule.getName(), rule);
                    }
                }
            }
        }
    }

    public String normalizeRuleName(String name)
    {
        return name.replace(".groovy", "");
    }

    private List<String> getResourceRuleFiles( String path ) throws IOException
    {
        List<String> filenames = new ArrayList<>();
        try ( InputStream in = this.getClass().getClassLoader().getResourceAsStream(path))
        {
            if ( in != null ) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String resource;
                while ((resource = br.readLine()) != null) {
                    filenames.add(resource);
                }
            }
        }
        return filenames;
    }

    private String getResourceRuleAsString( String name ) throws IOException {
        try ( InputStream in = this.getClass().getClassLoader().getResourceAsStream(RULES_DIR + "/" + name ) )
        {
            return IOUtils.toString(in, Charset.defaultCharset() );
        }
    }

    private void parseRuleSets()
            throws PromotionValidationException
    {
        File dataDir = dataFileManager.getDataFile( RULES_SETS_DIR );
        logger.info( "Scanning {} for promotion validation rule-set mappings...", dataDir );
        if ( dataDir.exists() )
        {
            final File[] scripts = dataDir.listFiles( ( pathname ) -> {
                logger.debug( "Checking for promotion rule-set in: {}", pathname );
                return pathname.getName().endsWith( ".json" );
            } );

            if ( scripts.length > 0 )
            {

                for ( final File script : scripts )
                {
                    logger.debug( "Reading promotion validation rule-set from: {}", script );
                    final ValidationRuleSet set = ruleParser.parseRuleSet( script );
                    if ( set != null )
                    {
                        ruleSets.put( script.getName(), set );
                    }
                }
            }
            else
            {
                logger.warn( "No rule-set json file was defined for promotion: no json file found in {} directory",
                             RULES_SETS_DIR );
            }
        }
        else
        {
            logger.warn( "No rule-set json file was defined for promotion: {} directory not exists", RULES_SETS_DIR );
        }
    }

    public ValidationCatalogDTO toDTO()
    {
        final Map<String, ValidationRuleDTO> rules = new HashMap<>();
        for ( final ValidationRuleMapping mapping : ruleMappings.values() )
        {
            rules.put( mapping.getName(), mapping.toDTO() );
        }

        return new ValidationCatalogDTO( enabled, rules, ruleSets );
    }

    public Set<ValidationRuleMapping> getRuleMappings()
    {
        return new HashSet<>( ruleMappings.values() );
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public ValidationRuleSet getRuleSetMatching( StoreKey storeKey )
    {
        if ( ruleSets == null )
        {
            logger.debug( "No rule sets to match against. No validations will be executed for: {}", storeKey );
            return null;
        }

        // Add deprecated form of StoreKey to the check to handle older rules.
        List<String> keyStrings = Arrays.asList( storeKey.toString(),
                                                 String.format( "%s:%s", storeKey.getType().singularEndpointName(),
                                                                storeKey.getName() ) );

        for ( Map.Entry<String, ValidationRuleSet> entry : ruleSets.entrySet() )
        {
            for ( String keyStr : keyStrings )
            {
                logger.debug( "Checking for rule-set match. Key='{}', rule-set: '{}'", keyStr, entry.getKey() );
                if ( entry.getValue().matchesKey( keyStr ) )
                {
                    logger.debug( "Rule set '{}' matches key '{}'", entry.getKey(), keyStr );
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public ValidationRule getRuleNamed( final String name )
    {
        final ValidationRuleMapping mapping = getRuleMappingNamed( name );
        return mapping == null ? null : mapping.getRule();
    }

    public Optional<ValidationRuleDTO> getNamedRuleAsDTO( final String name )
    {
        final Map<String, ValidationRuleDTO> rules = toDTO().getRules();
        ValidationRuleDTO dto = rules.get( name );
        if ( dto == null )
        {
            dto = rules.get( name + ".groovy" );
        }
        return dto == null ? Optional.empty() : Optional.of( dto );
    }

    public Optional<ValidationRuleSet> getNamedRuleSet( final String name )
    {
        final Map<String, ValidationRuleSet> ruleSets = toDTO().getRuleSets();
        ValidationRuleSet ruleSet = ruleSets.get( name );
        if ( ruleSet == null )
        {
            ruleSet = ruleSets.get( name + ".json" );
        }
        return ruleSet == null ? Optional.empty() : Optional.of( ruleSet );
    }

    public ValidationRuleMapping getRuleMappingNamed( final String name )
    {
        return ruleMappings.get( name );
    }

}
