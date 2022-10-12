package org.commonjava.service.promote.client.storage;

import java.util.Set;

public class BatchDeleteRequest
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
        return "BatchDeleteRequest{" +
                "paths=" + paths +
                ", filesystem='" + filesystem + '\'' +
                '}';
    }
}
