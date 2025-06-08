package net.conczin.immersive_furniture.config;

import net.conczin.immersive_furniture.Common;

import java.util.LinkedList;
import java.util.List;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

    public List<String> favorites = new LinkedList<>();
    public String immersiveLibraryUrl = "http://localhost:8000";

    // How many times the same furniture needs to be placed before low-memory mode is activated.
    // That mode is limited to 1024 unique furniture and cannot be cleaned up again.
    // All other furniture is more expensive since they require a block entity.
    public int lowMemoryModeThreshold = 10;

    // Furniture data is somewhat large and not deduplicated by default.
    // By only storing the hash and using a separate registry, this can be resolved.
    // This speeds up networking, world saving, loading, and memory usage.
    public boolean saveAsHash = true;

    // Cost multiplier for furniture crafting costs.
    public float costMultiplier = 1.0f;

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }
}
