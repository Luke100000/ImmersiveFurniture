package net.conczin.immersive_furniture.data.api.responses;

import com.google.gson.JsonObject;

public record IsAuthResponse(boolean authenticated) implements Response {
    public IsAuthResponse(JsonObject json) {
        this(json.get("authenticated").getAsBoolean());
    }
}
