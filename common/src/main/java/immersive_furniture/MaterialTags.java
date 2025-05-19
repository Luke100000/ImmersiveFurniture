package immersive_furniture;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MaterialTags {
    // TODO: Are tags required?
    public static final TagKey<Block> WOOL = create("wool");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, Common.locate(name));
    }
}

