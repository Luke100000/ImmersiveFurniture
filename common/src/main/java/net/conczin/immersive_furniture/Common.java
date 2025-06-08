package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.network.ClientHandler;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Common {
    public static final String MOD_ID = "immersive_furniture";

    public static ClientHandler clientHandler = new ClientHandler() {
    };

    public static Logger logger = LogManager.getLogger(MOD_ID);

    public static ResourceLocation locate(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public interface RegisterHelper<T> {
        void register(ResourceLocation name, T value);
    }
}
