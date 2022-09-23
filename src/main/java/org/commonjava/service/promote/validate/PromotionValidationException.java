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

import org.commonjava.service.promote.exception.PromotionException;
import org.commonjava.service.promote.model.PromoteRequest;
import org.commonjava.service.promote.model.StoreKey;

import java.io.IOException;

public class PromotionValidationException
    extends PromotionException
{

    public PromotionValidationException(String s, Exception e) {
        super(s, e);
    }

    public PromotionValidationException(String s, Exception e, StoreKey source, String message) {
        super(s, e, source, message);
    }

    public PromotionValidationException(String s, Exception e, String scriptName, String simpleName, String message) {
        super(s, e);
    }

    public PromotionValidationException(String s) {
        super(s, null);
    }

    public PromotionValidationException(String s, Exception e, String name, PromoteRequest request, Exception e1) {
        super(s, e);
    }

    public PromotionValidationException(String s, String verifyStores) {
        super(s, null);
    }
}
