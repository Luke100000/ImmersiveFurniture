package net.conczin.immersive_furniture.recipe;

import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import static net.conczin.immersive_furniture.item.Items.CRAFTING_MATERIAL;
import static net.conczin.immersive_furniture.item.Items.FURNITURE;

public class RecyclingRecipe extends CustomRecipe {
    public RecyclingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasFurniture = false;
        for (int j = 0; j < input.size(); j++) {
            ItemStack itemstack = input.getItem(j);
            if (itemstack.isEmpty()) {
                continue;
            }
            if (!itemstack.is(FURNITURE)) {
                return false;
            }
            hasFurniture = true;
        }
        return hasFurniture;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        int amount = 0;
        for (int j = 0; j < input.size(); j++) {
            ItemStack itemstack = input.getItem(j);
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
