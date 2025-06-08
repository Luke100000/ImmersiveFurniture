package net.conczin.immersive_furniture.data.api.responses;

import com.google.gson.JsonObject;

public record SuccessResponse() implements Response {
    public SuccessResponse(JsonObject ignoredJson) {
        this();
    }
}
