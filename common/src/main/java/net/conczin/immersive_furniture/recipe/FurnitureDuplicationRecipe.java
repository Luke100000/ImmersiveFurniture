package net.conczin.immersive_furniture.recipe;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.item.FurnitureItem;
import net.minecraft.core.NonNullList;
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

public class FurnitureDuplicationRecipe extends CustomRecipe {
    public FurnitureDuplicationRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack template = ItemStack.EMPTY;
        int materials = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(FURNITURE)) {
                if (!template.isEmpty()) {
                    return false;
                }
                template = stack;
            } else if (stack.is(CRAFTING_MATERIAL)) {
                materials += stack.getCount();
            } else {
                return false;
            }
        }

        if (template.isEmpty()) {
            return false;
        }

        FurnitureData data = FurnitureItem.getData(template);
        return data != FurnitureData.EMPTY && materials >= data.getCost();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack template = findTemplate(container);
        if (template.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = template.copy();
        result.setCount(1);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        int materialsToConsume = getCost(container);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(FURNITURE)) {
                ItemStack template = stack.copy();
                template.setCount(1);
                remaining.set(i, template);
            } else if (stack.is(CRAFTING_MATERIAL)) {
                int consumed = Math.min(stack.getCount(), materialsToConsume);
                materialsToConsume -= consumed;
                if (consumed > 0) {
                    // Vanilla removes one item from each non-empty crafting slot after this returns.
                    stack.setCount(stack.getCount() - consumed + 1);
                } else {
                    ItemStack material = stack.copy();
                    material.setCount(1);
                    remaining.set(i, material);
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.FURNITURE_DUPLICATION;
    }

    private static ItemStack findTemplate(CraftingContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(FURNITURE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int getCost(CraftingContainer container) {
        ItemStack template = findTemplate(container);
        if (template.isEmpty()) {
            return 0;
        }
        return FurnitureItem.getData(template).getCost();
    }
}
