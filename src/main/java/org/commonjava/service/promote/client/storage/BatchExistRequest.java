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
package org.commonjava.service.promote.client.storage;

import java.util.Set;

public class BatchExistRequest
{
    private Set<String> paths;

    private String filesystem;

    public Set<String> getPaths() {
        return paths;
    }

    public void setPaths(Set<String> paths) {
        this.paths = paths;
    }

    public String getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(String filesystem) {
        this.filesystem = filesystem;
    }

    @Override
    public String toString() {
        return "BatchExistRequest{" +
                "paths=" + paths +
                ", filesystem='" + filesystem + '\'' +
                '}';
    }
}
