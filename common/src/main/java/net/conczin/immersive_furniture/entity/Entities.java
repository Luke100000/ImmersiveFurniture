package net.conczin.immersive_furniture.entity;

import net.conczin.immersive_furniture.Common;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

public interface Entities {
    EntityType<SittingEntity> SITTING = EntityType.Builder
            .of((EntityType<SittingEntity> type, Level level) -> new SittingEntity(type, level), MobCategory.MISC)
            .sized(0.1f, 0.1f)
            .clientTrackingRange(64)
            .updateInterval(20)
            .fireImmune()
            .build("sitting");

    static void registerEntities(Common.RegisterHelper<EntityType<?>> helper) {
        helper.register(Common.locate("sitting"), SITTING);
    }
}
