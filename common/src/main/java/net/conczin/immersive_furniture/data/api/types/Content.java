package net.conczin.immersive_furniture.data.api.types;

import java.util.HashSet;
import java.util.Set;

public record Content(
        int content_id,
        int user_id,
        String username,
        int likes,
        Set<String> tags,
        String title,
        int version,
        long created_at,
        long updated_at,
        boolean is_liked,
        Set<String> reports,
        String meta,
        String data
) implements Tagged {
    public Content {
        tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
        reports = reports == null ? new HashSet<>() : new HashSet<>(reports);
    }
}
