package net.conczin.immersive_furniture.client.renderer;

import net.conczin.immersive_furniture.entity.SittingEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SittingEntityRenderer extends EntityRenderer<SittingEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("empty");

    public SittingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SittingEntity livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(SittingEntity entity) {
        return TEXTURE;
    }
}
