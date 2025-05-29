package immersive_furniture.network.s2c;

import immersive_furniture.data.FurnitureDataManager;
import immersive_furniture.cobalt.network.Message;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.utils.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class FurnitureDataResponse extends Message {
    public String hash;
    public FurnitureData data;

    public FurnitureDataResponse(String hash, FurnitureData data) {
        this.hash = hash;
        this.data = data;
    }

    public FurnitureDataResponse(FriendlyByteBuf b) {
        this.hash = b.readUtf();
        this.data = new FurnitureData(Utils.fromBytes(b.readByteArray()));
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeUtf(hash);
        b.writeByteArray(Utils.toBytes(data.toTag()));
    }

    @Override
    public void receive(Player e) {
        FurnitureDataManager.save(data, new ResourceLocation("hash", hash));
    }
}
