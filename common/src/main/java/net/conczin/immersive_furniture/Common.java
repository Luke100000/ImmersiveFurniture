package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.network.NetworkManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Common {
    public static final String MOD_ID = "immersive_furniture";
    public static NetworkManager networkManager;

    public static ClientHandler clientHandler = new ClientHandler() {
    };

    public static Logger logger = LogManager.getLogger(MOD_ID);

    public static ResourceLocation locate(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
