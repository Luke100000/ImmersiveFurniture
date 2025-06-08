package net.conczin.immersive_furniture.data.api.responses;


import com.google.gson.JsonObject;
import net.conczin.immersive_furniture.data.api.types.LiteContent;

public record ContentListResponse(LiteContent[] contents) implements Response {
    public ContentListResponse(JsonObject json) {
        this(json.getAsJsonArray("contents").asList().stream()
                .map(content -> new LiteContent(content.getAsJsonObject()))
                .toArray(LiteContent[]::new)
        );
    }
}
