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

public enum ContentDigest {
    MD5( "MD5", ".md5"),
    SHA_512( "SHA-512", ".sha512"),
    SHA_384( "SHA-384", ".sha384"),
    SHA_256( "SHA-256", ".sha256"),
    SHA_1 ( "SHA-1", ".sha1");

    private String digestName;

    final private String fileExt;

    ContentDigest(final String digestName, String fileExt)
    {
        this.digestName = digestName;
        this.fileExt = fileExt;
    }

    public String digestName()
    {
        return digestName;
    }

    public String getFileExt() {
        return fileExt;
    }
}
