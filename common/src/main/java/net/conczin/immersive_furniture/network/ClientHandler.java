package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.network.s2c.FurnitureInteractMessage;

public interface ClientHandler {
    default void openScreen() {

    }

    default void handleFurnitureInteract(FurnitureInteractMessage message) {

    }
}
