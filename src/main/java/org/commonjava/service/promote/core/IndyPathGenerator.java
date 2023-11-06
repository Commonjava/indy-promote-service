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
package org.commonjava.service.promote.core;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.model.core.PathStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

import static org.commonjava.indy.model.core.PathStyle.hashed;

@ApplicationScoped
public class IndyPathGenerator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public IndyPathGenerator()
    {
    }

    /**
     * Get styled (hashed) path for raw path.
     * @param rawPath
     * @param pathStyle
     * @return styled path
     */
    public String getStyledPath(final String rawPath, final PathStyle pathStyle)
    {
        String path = rawPath;
        if ( hashed.equals(pathStyle) )
        {
            File f = new File( path );
            String dir = f.getParent();
            if ( dir == null )
            {
                dir = "/";
            }

            if ( dir.length() > 1 && dir.startsWith( "/" ) )
            {
                dir = dir.substring( 1 );
            }

            String digest = DigestUtils.sha256Hex( dir );

            logger.trace( "Using SHA-256 digest: '{}' for dir: '{}' of path: '{}'", digest, dir, path );

            // For example: 00/11/001122334455667788/gulp-size-1.3.0.tgz
            path = String.format( "%s/%s/%s/%s", digest.substring( 0, 2 ), digest.substring( 2, 4 ), digest, f.getName() );
        }
        return path;
    }
}
