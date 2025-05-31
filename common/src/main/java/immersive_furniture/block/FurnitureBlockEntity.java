package immersive_furniture.block;

import immersive_furniture.BlockEntityTypes;
import immersive_furniture.config.Config;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.FurnitureDataManager;
import immersive_furniture.item.FurnitureItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureBlockEntity extends BlockEntity implements Clearable {
    private String hash;
    private FurnitureData data;

    public FurnitureBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.FURNITURE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FurnitureBlockEntity blockEntity) {

    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FurnitureBlockEntity blockEntity) {
        // TODO: If required, this ticker is of higher precision than the random animate tick
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(FurnitureItem.FURNITURE)) {
            this.data = new FurnitureData(tag.getCompound(FurnitureItem.FURNITURE));
        } else if (tag.contains(FurnitureItem.FURNITURE_HASH)) {
            // Delay loading
            hash = tag.getString(FurnitureItem.FURNITURE_HASH);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (this.data != null) {
            if (Config.getInstance().saveAsHash) {
                FurnitureDataManager.save(data, new ResourceLocation("hash", this.data.getHash()));
                tag.putString(FurnitureItem.FURNITURE_HASH, this.data.getHash());
            } else {
                tag.put(FurnitureItem.FURNITURE, this.data.toTag());
            }
        } else if (hash != null) {
            tag.putString(FurnitureItem.FURNITURE_HASH, hash);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void clearContent() {
        // TODO: Clear inventory
    }

    public NonNullList<ItemStack> getItems() {
        return NonNullList.create();
    }

    public FurnitureData getData() {
        if (hash != null) {
            data = FurnitureDataManager.getData(new ResourceLocation("hash", hash), true);
            if (data != null) {
                hash = null;
            }
        }
        return data;
    }
}

