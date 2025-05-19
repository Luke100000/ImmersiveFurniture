package immersive_furniture.config;

import immersive_furniture.Common;

import java.util.LinkedList;
import java.util.List;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

    public List<String> favorites = new LinkedList<>();
    public String immersiveLibraryUrl = "http://localhost:8000";

    public Config() {
        super("default");
    }

    public Config(String name) {
        super(name);
    }

    public static Config getInstance() {
        return INSTANCE;
    }
}
