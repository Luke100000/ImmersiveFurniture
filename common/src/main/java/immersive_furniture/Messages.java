package immersive_furniture;

import immersive_furniture.cobalt.network.NetworkHandler;
import immersive_furniture.network.s2c.CraftRequest;

public class Messages {
    public static void loadMessages() {
        NetworkHandler.registerMessage(CraftRequest.class, CraftRequest::new);
    }
}
