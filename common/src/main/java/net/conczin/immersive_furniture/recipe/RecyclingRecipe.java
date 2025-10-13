package net.conczin.immersive_furniture.recipe;

import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import static net.conczin.immersive_furniture.item.Items.CRAFTING_MATERIAL;
import static net.conczin.immersive_furniture.item.Items.FURNITURE;

public class RecyclingRecipe extends CustomRecipe {
    public RecyclingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public boolean matches(CraftingContainer inv, Level level) {
        for (int j = 0; j < inv.getContainerSize(); j++) {
            if (inv.getItem(j).is(FURNITURE)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int amount = 0;
        for (int j = 0; j < container.getContainerSize(); j++) {
            ItemStack itemstack = container.getItem(j);
            if (itemstack.is(FURNITURE)) {
                amount += FurnitureItem.getData(itemstack).getCost();
            }
        }
        return new ItemStack(CRAFTING_MATERIAL, amount);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.RECYCLING;
    }
}
