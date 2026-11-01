package net.conczin.immersive_furniture.data.api.types;

import java.util.HashSet;
import java.util.Set;

public record LiteContent(
        int content_id,
        int user_id,
        String username,
        int likes,
        Set<String> tags,
        String title,
        int version,
        boolean is_liked,
        Set<String> reports
) implements Tagged {
    public LiteContent {
        tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
        reports = reports == null ? new HashSet<>() : new HashSet<>(reports);
    }
}
