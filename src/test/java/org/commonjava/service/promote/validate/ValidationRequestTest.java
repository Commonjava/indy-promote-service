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
