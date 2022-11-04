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

public class PromotionValidationException extends Exception
{
    private String ruleName;

    public PromotionValidationException() {
    }

    public PromotionValidationException(String s, Exception e) {
        super(s, e);
    }

    public PromotionValidationException(String s, Exception e, String ruleName) {
        super(s, e);
        this.ruleName = ruleName;
    }

    public PromotionValidationException(String s) {
        super(s);
    }
}
