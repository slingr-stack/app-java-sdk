package io.slingr.api.app;

public enum Environment {
    PROD("prod"), STAGING("staging"), DEV("dev");

    private String path;

    Environment(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
