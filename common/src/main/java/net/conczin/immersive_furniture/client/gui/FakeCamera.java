package net.conczin.immersive_furniture.client.gui;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

public class FakeCamera extends Camera {
    public void setup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, float yaw, float pitch) {
        super.setup(level, entity, detached, thirdPersonReverse, partialTick);

        setPosition(0.0, 1024.0, 0.0);
        setRotation((float) (yaw * 180.0 / Math.PI + 180), (float) (-pitch * 180.0 / Math.PI));
    }
}
