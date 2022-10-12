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
