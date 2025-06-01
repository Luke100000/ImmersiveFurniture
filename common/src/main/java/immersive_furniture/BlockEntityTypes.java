package immersive_furniture;

import com.mojang.datafixers.types.Type;
import immersive_furniture.block.FurnitureBlockEntity;
import immersive_furniture.cobalt.registration.Registration;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public interface BlockEntityTypes {
    Supplier<BlockEntityType<FurnitureBlockEntity>> FURNITURE = register("furniture", () -> Registration.blockEntityTypeBuilder(FurnitureBlockEntity::new, Blocks.FURNITURE_ENTITY.get()));

    static <T extends BlockEntity> Supplier<BlockEntityType<T>> register(String name, Supplier<BlockEntityType.Builder<T>> type) {
        Type<?> datafixerType = Util.fetchChoiceType(References.BLOCK_ENTITY, name);
        assert datafixerType != null;
        return Registration.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Common.locate(name), () -> type.get().build(datafixerType));
    }

    static void bootstrap() {
    }
}
