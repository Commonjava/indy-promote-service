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

import org.commonjava.service.promote.model.PathsPromoteRequest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidationRequestTest {

    @Test
    public void testGetSourcePaths()
    {
        PathsPromoteRequest promoteRequest = new PathsPromoteRequest();
        Set<String> paths = new HashSet<>();
        paths.add("a/normal/path");
        paths.add("a/maven-metadata.xml");
        paths.add("a/checksum/path/foo.sha1");
        promoteRequest.setPaths(paths);
        ValidationRequest validationRequest = new ValidationRequest( promoteRequest, null, null );
        Set<String> sourcePaths = validationRequest.getSourcePaths();
        assertTrue(sourcePaths.size() == 1);
        assertTrue( sourcePaths.contains("a/normal/path"));

        sourcePaths = validationRequest.getSourcePaths( true, false );
        assertTrue(sourcePaths.size() == 2);
        assertTrue( sourcePaths.contains("a/normal/path"));
        assertTrue( sourcePaths.contains("a/maven-metadata.xml"));

        sourcePaths = validationRequest.getSourcePaths( true, true );
        assertTrue(sourcePaths.size() == 3);
        assertTrue( sourcePaths.containsAll(paths));
    }
}
