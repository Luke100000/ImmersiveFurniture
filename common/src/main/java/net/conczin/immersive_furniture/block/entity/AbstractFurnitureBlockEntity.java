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
    private int subOffsetZ = DEFAULT_OFFSET;

    protected AbstractFurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getSubOffsetX() {
        return subOffsetX;
    }

    @Override
    public int getSubOffsetZ() {
        return subOffsetZ;
    }

    @Override
    public void setSubOffset(int subOffsetX, int subOffsetZ, boolean sync) {
        int clampedX = clamp(subOffsetX);
        int clampedZ = clamp(subOffsetZ);
        if (clampedX == this.subOffsetX && clampedZ == this.subOffsetZ) {
            return;
        }
        this.subOffsetX = clampedX;
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
        tag.putInt("SubOffsetZ", subOffsetZ);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        subOffsetX = clamp(tag.getInt("SubOffsetX"));
        subOffsetZ = clamp(tag.getInt("SubOffsetZ"));
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
