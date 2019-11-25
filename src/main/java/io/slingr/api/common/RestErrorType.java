package io.slingr.api.common;

import io.slingr.api.common.Json;

/**
 * <p>Type of error that can happen in the RestClient
 *
 * <p>Created by lefunes on 14/05/15.
 */
public enum RestErrorType {
    GENERIC_ERROR(0, "Error"),
    ARGUMENT_EXCEPTION(1, "Argument invalid"),
    API_EXCEPTION(2, "API exception"),
    CONVERSION_EXCEPTION(3, "Conversion exception"),
    CLIENT_EXCEPTION(4, "Client handling exception");

    private final String code;
    private final String name;

    RestErrorType(int code, String name) {
        this.code = String.format("%02d", code);
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Json toJson() {
        Json json = Json.map();
        json.set("code", code);
        json.set("name", name);
        return json;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, code);
    }
}
