package net.conczin.immersive_furniture.mixin.client;

import net.conczin.immersive_furniture.client.model.DynamicAtlas;
import net.conczin.immersive_furniture.client.model.FurnitureModelBaker;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelShaper.class)
public class ItemModelShaperMixin {
    @Inject(method = "getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    private void immersiveFuture$getItemModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        if (stack.getItem() instanceof FurnitureItem) {
            // This is required not because of the model itself but because of the dynamic transformation
            cir.setReturnValue(FurnitureModelBaker.getModel(FurnitureItem.getData(stack), DynamicAtlas.ENTITY));
        }
    }
}
