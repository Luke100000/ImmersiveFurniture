package immersive_furniture.data.api.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.stream.Collectors;

public record Content(
        int contentid,
        int userid,
        String username,
        int likes,
        Set<String> tags,
        String title,
        int version,
        String meta,
        String data
) implements Tagged {
    public Content(JsonObject json) {
        this(
                json.get("contentid").getAsInt(),
                json.get("userid").getAsInt(),
                json.get("username").getAsString(),
                json.get("likes").getAsInt(),
                json.getAsJsonArray("tags").asList().stream()
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toSet()),
                json.get("title").getAsString(),
                json.get("version").getAsInt(),
                json.get("meta").getAsString(),
                json.get("data").getAsString()
        );
    }
}
