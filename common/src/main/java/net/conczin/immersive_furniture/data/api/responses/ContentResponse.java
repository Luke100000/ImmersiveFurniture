package net.conczin.immersive_furniture.data.api.responses;


import com.google.gson.JsonObject;
import net.conczin.immersive_furniture.data.api.types.Content;

public record ContentResponse(Content content) implements Response {
    public ContentResponse(JsonObject json) {
        this(new Content(json.getAsJsonObject("content")));
    }
}
