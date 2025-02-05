package immersive_furniture;

import immersive_furniture.network.ClientNetworkManager;

public class CommonClient {
    public static void postLoad() {
        Common.networkManager = new ClientNetworkManager();
    }
}
