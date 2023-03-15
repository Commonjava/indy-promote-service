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
package org.commonjava.service.promote.config;

import java.io.File;

public class TestPromoteConfig implements PromoteConfig {
    private File baseDir;

    public TestPromoteConfig() {
    }

    @Override
    public File baseDir() {
        return baseDir;
    }

    @Override
    public String callbackUri() {
        return null;
    }

    public TestPromoteConfig( File baseDir ) {
        this.baseDir = baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }
}
