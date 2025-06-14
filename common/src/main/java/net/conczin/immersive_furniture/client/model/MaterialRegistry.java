package net.conczin.immersive_furniture.client.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialRegistry {
    public static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public final Map<ResourceLocation, MaterialSource> materials = new ConcurrentHashMap<>();

    public void sync() {
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

        // This enforces a render since at this point the renderer could have caught missing textures
        DynamicAtlas.SCRATCH.clear();
    }
}
