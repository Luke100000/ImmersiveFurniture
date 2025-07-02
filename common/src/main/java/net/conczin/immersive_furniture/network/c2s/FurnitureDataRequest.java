package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record FurnitureDataRequest(String hash) implements ImmersivePayload {
    public FurnitureDataRequest(FriendlyByteBuf b) {
        this(b.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeUtf(hash);
    }

    @Override
    public void handle(Player e) {
        if (e instanceof ServerPlayer sp) {
            // Retrieve from hash storage
            FurnitureData data = FurnitureDataManager.getHashData(hash);
            if (data != null) {
                Network.sendToPlayer(new FurnitureDataResponse(hash, data), sp);
            } else {
                Common.logger.warn("Client requested missing furniture data for hash {}.", hash);
            }
        }
    }
}
