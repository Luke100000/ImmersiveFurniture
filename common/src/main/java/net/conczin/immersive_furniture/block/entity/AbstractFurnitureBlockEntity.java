package net.conczin.immersive_furniture.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;

public abstract class AbstractFurnitureBlockEntity extends BlockEntity implements FurnitureOffsetHolder {
    private int subOffsetX = DEFAULT_OFFSET;
    private int subOffsetY = DEFAULT_OFFSET;
    private int subOffsetZ = DEFAULT_OFFSET;

    protected AbstractFurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getSubOffsetX() {
        return subOffsetX;
    }

    @Override
    public int getSubOffsetY() {
        return subOffsetY;
    }

    @Override
    public int getSubOffsetZ() {
        return subOffsetZ;
    }

    @Override
    public void setSubOffset(int subOffsetX, int subOffsetY, int subOffsetZ, boolean sync) {
        int clampedX = clamp(subOffsetX);
        int clampedY = clamp(subOffsetY);
        int clampedZ = clamp(subOffsetZ);
        if (clampedX == this.subOffsetX && clampedY == this.subOffsetY && clampedZ == this.subOffsetZ) {
            return;
        }
        this.subOffsetX = clampedX;
        this.subOffsetY = clampedY;
        this.subOffsetZ = clampedZ;
        if (sync) {
            setChanged();
            if (level != null) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }

    private static int clamp(int value) {
        return Mth.clamp(value, 0, 16);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SubOffsetX", subOffsetX);
        tag.putInt("SubOffsetY", subOffsetY);
        tag.putInt("SubOffsetZ", subOffsetZ);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        subOffsetX = loadOffset(tag, "SubOffsetX");
        subOffsetY = loadOffset(tag, "SubOffsetY");
        subOffsetZ = loadOffset(tag, "SubOffsetZ");
    }

    private static int loadOffset(CompoundTag tag, String key) {
        return tag.contains(key) ? clamp(tag.getInt(key)) : DEFAULT_OFFSET;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
