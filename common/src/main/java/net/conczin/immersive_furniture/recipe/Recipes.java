package net.conczin.immersive_furniture.recipe;

import net.conczin.immersive_furniture.Common;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class Recipes {
    public static RecipeSerializer<RecyclingRecipe> RECYCLING = new SimpleCraftingRecipeSerializer<>(RecyclingRecipe::new);
    public static RecipeSerializer<FurnitureDuplicationRecipe> FURNITURE_DUPLICATION = new SimpleCraftingRecipeSerializer<>(FurnitureDuplicationRecipe::new);

    public static void registerRecipes(Common.RegisterHelper<RecipeSerializer<?>> helper) {
        helper.register(Common.locate("recycler"), RECYCLING);
        helper.register(Common.locate("furniture_duplication"), FURNITURE_DUPLICATION);
    }
}
