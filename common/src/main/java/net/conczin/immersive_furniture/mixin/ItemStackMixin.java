package net.conczin.immersive_furniture.mixin;

import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "isSameItemSameTags", at = @At("HEAD"), cancellable = true)
    private static void immersiveFurniture$compareFurnitureHash(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(other.getItem()) && stack.getItem() instanceof FurnitureItem) {
            cir.setReturnValue(FurnitureItem.isEqual(stack, other));
        }
    }
}
