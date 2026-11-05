package net.conczin.immersive_furniture.data.api.responses;

import net.conczin.immersive_furniture.data.api.types.LiteContent;

public record ContentListResponse(LiteContent[] contents, boolean has_more) implements Response {
}
