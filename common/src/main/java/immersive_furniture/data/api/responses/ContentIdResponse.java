package immersive_furniture.data.api.responses;

import com.google.gson.JsonObject;

public record ContentIdResponse(int contentid) implements Response {
    public ContentIdResponse(JsonObject json) {
        this(json.get("contentid").getAsInt());
    }
}
