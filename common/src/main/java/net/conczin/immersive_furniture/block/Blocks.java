package net.conczin.immersive_furniture.block;

import net.conczin.immersive_furniture.Common;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public interface Blocks {
    Block ARTISANS_WORKSTATION = new ArtisansWorkstationBlock(baseProps()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()
    );

    Block FURNITURE = new FurnitureBlock(baseFurnitureProps());

    Block FURNITURE_ENTITY = new EntityFurnitureBlock(baseFurnitureProps()
            .lightLevel((blockState) -> blockState.getValue(EntityFurnitureBlock.LIGHT))
    );

    Block FURNITURE_LIGHT = new LightFurnitureBlock(baseFurnitureProps()
            .lightLevel((blockState) -> blockState.getValue(LightFurnitureBlock.LIGHT) * 3)
    );

    Block FURNITURE_PROXY = new FurnitureProxyBlock(baseFurnitureProps()
            .noOcclusion()
    );

    static BlockBehaviour.Properties baseProps() {
        return BlockBehaviour.Properties.of();
    }

    static BlockBehaviour.Properties baseFurnitureProps() {
        return baseProps()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .noLootTable()
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.BLOCK)
                .dynamicShape();
    }

    static void registerBlocks(Common.RegisterHelper<Block> helper) {
        helper.register(Common.locate("artisans_workstation"), ARTISANS_WORKSTATION);
        helper.register(Common.locate("furniture"), FURNITURE);
        helper.register(Common.locate("furniture_entity"), FURNITURE_ENTITY);
        helper.register(Common.locate("furniture_light"), FURNITURE_LIGHT);
        helper.register(Common.locate("furniture_proxy"), FURNITURE_PROXY);
    }
}
