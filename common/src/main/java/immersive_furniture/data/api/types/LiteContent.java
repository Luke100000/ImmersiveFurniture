package immersive_furniture.data.api.types;

import com.google.gson.JsonObject;

import java.util.Set;

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
                Set.of(json.get("tags").getAsString().split(",")),
                json.get("title").getAsString(),
                json.get("version").getAsInt()
        );
    }
}
