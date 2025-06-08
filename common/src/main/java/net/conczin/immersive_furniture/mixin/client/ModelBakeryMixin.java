package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.data.MaterialRegistry;
import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiFunction;

@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Shadow
    @Final
    private Map<ResourceLocation, BlockModel> modelResources;

    @Unique
    private boolean immersive_furniture$isCube(BlockModel model) {
        while (model != null) {
            if (model.name.equals("minecraft:block/cube")) {
                return true;
            }
            model = ((BlockModelAccessor) model).getParent();
        }
        return false;
    }

    @Inject(method = "bakeModels(Ljava/util/function/BiFunction;)V", at = @At("HEAD"))
    private void onBakeModels(BiFunction<ResourceLocation, Material, TextureAtlasSprite> biFunction, CallbackInfo ci) {
        // Remember all the cube models as potential material sources
        for (BlockModel model : modelResources.values()) {
            if (model.name.contains(":block/") && immersive_furniture$isCube(model)) {
                MaterialSource source = MaterialSource.create(model);
                if (source != null) {
                    MaterialRegistry.INSTANCE.register(source);
                }
            }
        }
    }
}
