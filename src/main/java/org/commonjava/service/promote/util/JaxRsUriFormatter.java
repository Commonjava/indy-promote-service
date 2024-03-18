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

import org.commonjava.service.promote.model.PackageTypeDescriptor;
import org.commonjava.service.promote.model.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriInfo;

public class JaxRsUriFormatter

{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static String getBaseUrlByStoreKey( UriInfo uriInfo, StoreKey storeKey )
    {
        PackageTypeDescriptor typeDescriptor = PackageTypes.getPackageTypeDescriptor( storeKey.getPackageType() );
        return uriInfo.getBaseUriBuilder()
                      .path( typeDescriptor.getContentRestBasePath() )
                      .path( storeKey.getType().singularEndpointName() )
                      .path( storeKey.getName() )
                      .build()
                      .toString();
    }
}
