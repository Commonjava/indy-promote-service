package org.commonjava.service.promote.client.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

public class FileCopyRequest
{
    private Set<String> paths;

    @JsonIgnore
    private boolean failWhenExists;

    private String sourceFilesystem;

    private String targetFilesystem;

    public Set<String> getPaths() {
        return paths;
    }

    public void setPaths(Set<String> paths) {
        this.paths = paths;
    }

    public boolean isFailWhenExists() {
        return failWhenExists;
    }

    public void setFailWhenExists(boolean failWhenExists) {
        this.failWhenExists = failWhenExists;
    }

    public String getSourceFilesystem() {
        return sourceFilesystem;
    }

    public void setSourceFilesystem(String sourceFilesystem) {
        this.sourceFilesystem = sourceFilesystem;
    }

    public String getTargetFilesystem() {
        return targetFilesystem;
    }

    public void setTargetFilesystem(String targetFilesystem) {
        this.targetFilesystem = targetFilesystem;
    }
}
