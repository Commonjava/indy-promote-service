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

import static java.util.Collections.emptySet;

public class BatchDeleteResult
{
    private String filesystem;

    private Set<String> succeeded;

    private Set<String> failed;

    public String getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(String filesystem) {
        this.filesystem = filesystem;
    }

    public Set<String> getSucceeded() {
        return succeeded == null ? emptySet() : succeeded;
    }

    public void setSucceeded(Set<String> succeeded) {
        this.succeeded = succeeded;
    }

    public Set<String> getFailed() {
        return failed == null ? emptySet() : failed;
    }

    public void setFailed(Set<String> failed) {
        this.failed = failed;
    }

    @Override
    public String toString() {
        return "BatchDeleteResult{" +
                "filesystem='" + filesystem + '\'' +
                ", succeeded=" + succeeded +
                ", failed=" + failed +
                '}';
    }
}
