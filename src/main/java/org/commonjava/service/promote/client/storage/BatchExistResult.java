package org.commonjava.service.promote.client.storage;

import java.util.Set;

public class BatchExistResult
{
    private String filesystem;

    private Set<String> missing;

    public String getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(String filesystem) {
        this.filesystem = filesystem;
    }

    public Set<String> getMissing() {
        return missing;
    }

    public void setMissing(Set<String> missing) {
        this.missing = missing;
    }

    @Override
    public String toString() {
        return "BatchExistResult{" +
                "filesystem='" + filesystem + '\'' +
                ", missing=" + missing +
                '}';
    }
}
