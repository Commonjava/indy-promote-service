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
