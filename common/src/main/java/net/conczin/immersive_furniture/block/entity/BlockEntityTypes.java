package net.conczin.immersive_furniture.block.entity;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTypes {
    public static BlockEntityType<FurnitureBlockEntity> FURNITURE;

    public interface TriFunction<E extends BlockEntity> {
        BlockEntityType<E> apply(ResourceLocation name, BlockEntitySupplier<E> constructor, Block block);
    }

    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(TriFunction register) {
        FURNITURE = register.apply(
                Common.locate("furniture"),
                FurnitureBlockEntity::new,
                Blocks.FURNITURE_ENTITY
        );
    }
}
