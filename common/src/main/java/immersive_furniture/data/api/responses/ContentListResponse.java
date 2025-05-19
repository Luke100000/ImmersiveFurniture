package immersive_furniture.data.api.responses;


import com.google.gson.JsonObject;
import immersive_furniture.data.api.types.LiteContent;

import java.util.Arrays;

public record ContentListResponse(LiteContent[] contents) implements Response {
    public ContentListResponse(JsonObject json) {
        this(json.getAsJsonArray("contents").asList().stream()
                .map(content -> new LiteContent(content.getAsJsonObject()))
                .toArray(LiteContent[]::new)
        );
    }
}
