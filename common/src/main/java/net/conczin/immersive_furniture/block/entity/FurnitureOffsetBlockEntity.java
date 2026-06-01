package net.conczin.immersive_furniture.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FurnitureOffsetBlockEntity extends AbstractFurnitureBlockEntity {
    public FurnitureOffsetBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.FURNITURE_OFFSET, pos, state);
    }
}
