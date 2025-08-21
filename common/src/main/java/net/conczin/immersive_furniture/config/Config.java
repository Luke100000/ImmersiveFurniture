package net.conczin.immersive_furniture.config;

import net.conczin.immersive_furniture.Common;

import java.util.LinkedList;
import java.util.List;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

    public List<String> favorites = new LinkedList<>();
    public String immersiveLibraryUrl = "https://mca.conczin.net";


    public int maximumInteractDistance = 128;

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

    // Interval in seconds between autosaves in the editor. Negative value disables autosave.
    public int autosaveInterval = 60;

    // The maximum number of mipmap levels supported (0 to disable, 3 for maximum quality).
    // Higher values reduce the maximum amount of unique furniture that can be rendered fast.
    // Lower values may lead to artifacts on steep view angles.
    public int maxMipLevel = 2;

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }
}
