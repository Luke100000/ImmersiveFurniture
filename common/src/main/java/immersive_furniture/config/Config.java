package immersive_furniture.config;

import immersive_furniture.Common;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(Common.MOD_ID), Config.class);

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
