package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.network.s2c.FurnitureInteractMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public interface ClientHandler {
    default void openScreen() {

    }

    default void handleFurnitureInteract(FurnitureInteractMessage message) {

    }

    default void playFurnitureSound(BlockPos pos, Vec3 soundPos, SoundEvent sound, float volume, float pitch, RandomSource random) {

    }

    default void stopFurnitureSounds(BlockPos pos) {

    }

    default void stopAllFurnitureSounds() {

    }
}
