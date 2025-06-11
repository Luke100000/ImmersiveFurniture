package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class MaterialRegistry {
    public static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public final Map<ResourceLocation, MaterialSource> materials = new HashMap<>();

    public void sync() {
        materials.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.getRenderShape() == RenderShape.MODEL) {
                MaterialSource source = MaterialSource.create(state);
                if (source != null) {
                    register(source);
                }
            }
        }
    }

    public void register(MaterialSource material) {
        materials.put(material.location(), material);
    }
}
