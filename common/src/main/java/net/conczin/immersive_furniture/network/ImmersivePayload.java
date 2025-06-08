package net.conczin.immersive_furniture.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public interface ImmersivePayload {
    void encode(FriendlyByteBuf b);

    void handle(Player e);
}