package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.conczin.immersive_furniture.client.FurnitureSoundInstance;
import net.conczin.immersive_furniture.client.gui.ArtisansWorkstationLibraryScreen;
import net.conczin.immersive_furniture.network.s2c.FurnitureInteractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ClientHandlerImpl implements ClientHandler {
    private final Map<BlockPos, List<FurnitureSoundInstance>> furnitureSounds = new HashMap<>();

    @Override
    public void openScreen() {
        Minecraft.getInstance().setScreen(new ArtisansWorkstationLibraryScreen());
    }

    @Override
    public void handleFurnitureInteract(FurnitureInteractMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        BlockState blockState = level.getBlockState(message.pos());
        if (blockState.getBlock() instanceof BaseFurnitureBlock furnitureBlock) {
            furnitureBlock.onInteract(level, blockState, message.pos(), message.active(), minecraft.player);
        }
    }

    @Override
    public void playFurnitureSound(BlockPos pos, Vec3 soundPos, SoundEvent sound, float volume, float pitch, RandomSource random) {
        Minecraft minecraft = Minecraft.getInstance();
        FurnitureSoundInstance instance = new FurnitureSoundInstance(sound, volume, pitch, random, soundPos);
        List<FurnitureSoundInstance> sounds = furnitureSounds.computeIfAbsent(pos.immutable(), ignored -> new LinkedList<>());
        sounds.removeIf(existing -> !minecraft.getSoundManager().isActive(existing));
        sounds.add(instance);
        minecraft.getSoundManager().play(instance);
    }

    @Override
    public void stopFurnitureSounds(BlockPos pos) {
        List<FurnitureSoundInstance> sounds = furnitureSounds.remove(pos);
        if (sounds == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        for (FurnitureSoundInstance sound : sounds) {
            sound.stopPlayback();
            minecraft.getSoundManager().stop(sound);
        }
    }

    @Override
    public void stopAllFurnitureSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        for (List<FurnitureSoundInstance> sounds : furnitureSounds.values()) {
            for (FurnitureSoundInstance sound : sounds) {
                sound.stopPlayback();
                minecraft.getSoundManager().stop(sound);
            }
        }
        furnitureSounds.clear();
    }

}
