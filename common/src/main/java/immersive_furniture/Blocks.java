package immersive_furniture;

import immersive_furniture.block.*;
import immersive_furniture.cobalt.registration.Registration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

public interface Blocks {
    Supplier<Block> ARTISANS_WORKSTATION = register("artisans_workstation", () -> new ArtisansWorkstationBlock(baseProps()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()
    ));

    Supplier<Block> FURNITURE = register("furniture", () -> new FurnitureBlock(baseFurnitureProps()));

    Supplier<Block> FURNITURE_ENTITY = register("furniture_entity", () -> new EntityFurnitureBlock(baseFurnitureProps()
            .lightLevel((blockState) -> blockState.getValue(EntityFurnitureBlock.LIGHT))
    ));

    Supplier<Block> FURNITURE_LIGHT = register("furniture_light", () -> new LightFurnitureBlock(baseFurnitureProps()
            .lightLevel((blockState) -> blockState.getValue(LightFurnitureBlock.LIGHT) * 3)
    ));

    static Supplier<Block> register(String name, Supplier<Block> block) {
        return Registration.register(BuiltInRegistries.BLOCK, Common.locate(name), block);
    }

    static void bootstrap() {
    }

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
}
