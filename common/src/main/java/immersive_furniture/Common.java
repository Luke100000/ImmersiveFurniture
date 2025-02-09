package immersive_furniture;

import immersive_furniture.network.NetworkManager;
import net.minecraft.resources.ResourceLocation;

public final class Common {
    public static final String SHORT_MOD_ID = "ic_fu";
    public static final String MOD_ID = "immersive_furniture";
    public static String MOD_LOADER = "unknown";
    public static NetworkManager networkManager;

    public static ClientHandler clientHandler = new ClientHandler() {
    };

    public static ResourceLocation locate(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
