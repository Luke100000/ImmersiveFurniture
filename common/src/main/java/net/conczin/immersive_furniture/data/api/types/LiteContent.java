package net.conczin.immersive_furniture.data.api.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.stream.Collectors;

public record LiteContent(
        int contentid,
        int userid,
        String username,
        int likes,
        Set<String> tags,
        String title,
        int version
) implements Tagged {
    public LiteContent(JsonObject json) {
        this(
                json.get("contentid").getAsInt(),
                json.get("userid").getAsInt(),
                json.get("username").getAsString(),
                json.get("likes").getAsInt(),
                json.getAsJsonArray("tags").asList().stream()
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toSet()),
                json.get("title").getAsString(),
                json.get("version").getAsInt()
        );
    }
}
