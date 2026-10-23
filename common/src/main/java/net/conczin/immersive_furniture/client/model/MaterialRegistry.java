package net.conczin.immersive_furniture.client.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class MaterialRegistry {
    public static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public volatile Map<ResourceLocation, MaterialSource> materials = Map.of();

    public void sync() {
        Map<ResourceLocation, MaterialSource> materials = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.getRenderShape() == RenderShape.MODEL) {
                try {
                    MaterialSource source = MaterialSource.create(state);
                    if (source != null) {
                        materials.put(source.location(), source);
                    }
                } catch (Exception e) {
                    // Some blocks have unsafe dynamic generation, let's safely ignore them here
                }
            }
        }
        this.materials = Map.copyOf(materials);
    }
}
