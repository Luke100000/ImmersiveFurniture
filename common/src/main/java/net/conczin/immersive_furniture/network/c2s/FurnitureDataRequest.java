package net.conczin.immersive_furniture.network.c2s;

import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class FurnitureDataRequest implements ImmersivePayload {
    public String hash;

    public FurnitureDataRequest(String hash) {
        this.hash = hash;
    }

    public FurnitureDataRequest(FriendlyByteBuf b) {
        this.hash = b.readUtf();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeUtf(hash);
    }

    @Override
    public void handle(Player e) {
        if (e instanceof ServerPlayer sp) {
            FurnitureData data = FurnitureDataManager.getData(hash);

            // Cache damaged, it's better to return fallback data
            if (data == null) data = FurnitureData.EMPTY;

            Network.sendToPlayer(new FurnitureDataResponse(hash, data), sp);
        }
    }
}
