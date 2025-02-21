package immersive_furniture.client.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialRegistry {
    public static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public final Map<ResourceLocation, MaterialSource> materials = new HashMap<>();

    public void register(MaterialSource material) {
        materials.put(material.location(), material);
    }

    public static List<SoundType> getSoundTypes() {
        return BuiltInRegistries.BLOCK.stream().map(Block::defaultBlockState).map(BlockState::getSoundType).distinct().toList();
    }
}
