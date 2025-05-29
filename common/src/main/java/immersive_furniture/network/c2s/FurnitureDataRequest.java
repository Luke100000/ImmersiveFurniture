package immersive_furniture.network.c2s;

import immersive_furniture.data.FurnitureDataManager;
import immersive_furniture.cobalt.network.Message;
import immersive_furniture.cobalt.network.NetworkHandler;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.network.s2c.FurnitureDataResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class FurnitureDataRequest extends Message {
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
    public void receive(Player e) {
        if (e instanceof ServerPlayer sp) {
            FurnitureData data = FurnitureDataManager.getData(hash);
            NetworkHandler.sendToPlayer(new FurnitureDataResponse(hash, data), sp);
        }
    }
}
