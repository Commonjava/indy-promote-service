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

import org.commonjava.service.promote.model.PromoteRequest;
import org.commonjava.service.promote.model.StoreKey;
import org.commonjava.service.promote.model.ValidationRuleSet;

import java.util.regex.Pattern;

public class ValidationRequest
{
    private final PromoteRequest promoteRequest;

    private final ValidationRuleSet ruleSet;

    private final StoreKey sourceRepository;

    private static final String VERSION_PATTERN = "versionPattern";

    private static final String SCOPED_VERSION_PATTERN = "scopedVersionPattern";

    private final PromotionValidationTools tools;

    public ValidationRequest( PromoteRequest promoteRequest, ValidationRuleSet ruleSet, PromotionValidationTools tools )
    {
        this.promoteRequest = promoteRequest;
        this.ruleSet = ruleSet;
        this.sourceRepository = promoteRequest.getSource();
        this.tools = tools;
    }

    public PromoteRequest getPromoteRequest()
    {
        return promoteRequest;
    }

    public ValidationRuleSet getRuleSet()
    {
        return ruleSet;
    }

    public String getValidationParameter( String key )
    {
        return ruleSet.getValidationParameter( key );
    }

    public Pattern getVersionPattern( String key )
    {
        return ruleSet.getVersionPattern( key );
    }

    public Pattern getVersionPattern()
    {
        return ruleSet.getVersionPattern( VERSION_PATTERN );
    }

    public Pattern getScopedVersionPattern( String key )
    {
        return ruleSet.getScopedVersionPattern( key );
    }

    public Pattern getScopedVersionPattern()
    {
        return ruleSet.getScopedVersionPattern( SCOPED_VERSION_PATTERN );
    }

    public StoreKey getSource()
    {
        return sourceRepository;
    }

    public StoreKey getTarget()
    {
        return promoteRequest.getTarget();
    }

    public PromotionValidationTools getTools() {
        return tools;
    }
}
