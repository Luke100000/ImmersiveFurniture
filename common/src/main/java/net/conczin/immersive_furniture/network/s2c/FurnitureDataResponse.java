package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.utils.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record FurnitureDataResponse(String hash, FurnitureData data) implements ImmersivePayload {
    public FurnitureDataResponse(FriendlyByteBuf b) {
        this(b.readUtf(), new FurnitureData(Utils.fromBytes(b.readByteArray())));
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeUtf(hash);
        b.writeByteArray(Utils.toBytes(data.toTag()));
    }

    @Override
    public void handle(Player e) {
        FurnitureDataManager.save(data, new ResourceLocation("cache", hash));
    }
}
