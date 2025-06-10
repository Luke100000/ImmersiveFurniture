package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.conczin.immersive_furniture.data.MaterialRegistry;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Unique
    private static boolean immersive_furniture$isCube(BlockModel model) {
        while (model != null) {
            if (model.name.equals("minecraft:block/cube")) {
                return true;
            }
            model = ((BlockModelAccessor) model).getParent();
        }
        return false;
    }

    @Inject(method = "<init>(Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiling/ProfilerFiller;Ljava/util/Map;Ljava/util/Map;)V", at = @At("RETURN"))
    private void immersiveFurniture$cinit(BlockColors blockColors, ProfilerFiller profilerFiller, Map<ResourceLocation, BlockModel> modelResources, Map<ResourceLocation, List<ModelBakery.LoadedJson>> blockStateResources, CallbackInfo ci) {
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
