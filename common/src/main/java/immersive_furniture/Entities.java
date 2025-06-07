package immersive_furniture;

import immersive_furniture.cobalt.registration.Registration;
import immersive_furniture.entity.SittingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public interface Entities {
    Supplier<EntityType<SittingEntity>> SITTING = register("sitting", EntityType.Builder
            .of((EntityType<SittingEntity> type, Level level) -> new SittingEntity(type, level), MobCategory.MISC)
            .sized(0.1f, 0.1f)
            .clientTrackingRange(64)
            .updateInterval(20)
            .fireImmune()
    );

    static void bootstrap() {

    }

    static <T extends Entity> Supplier<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        ResourceLocation id = Common.locate(name);
        return Registration.register(BuiltInRegistries.ENTITY_TYPE, id, () -> builder.build(id.toString()));
    }
}
