package net.conczin.immersive_furniture;

import net.conczin.immersive_furniture.cobalt.network.NetworkHandler;
import net.conczin.immersive_furniture.network.c2s.FurnitureDataRequest;
import net.conczin.immersive_furniture.network.s2c.CraftRequest;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.conczin.immersive_furniture.network.s2c.FurnitureRegistryMessage;

public class Messages {
    public static void loadMessages() {
        NetworkHandler.registerMessage(CraftRequest.class, CraftRequest::new);
        NetworkHandler.registerMessage(FurnitureDataRequest.class, FurnitureDataRequest::new);
        NetworkHandler.registerMessage(FurnitureDataResponse.class, FurnitureDataResponse::new);
        NetworkHandler.registerMessage(FurnitureRegistryMessage.class, FurnitureRegistryMessage::new);
    }
}
