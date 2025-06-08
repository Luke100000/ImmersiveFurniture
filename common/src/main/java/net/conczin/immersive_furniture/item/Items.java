package net.conczin.immersive_furniture.item;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public interface Items {
    Item ARTISANS_WORKSTATION = new BlockItem(Blocks.ARTISANS_WORKSTATION, baseProps());
    Item FURNITURE = new FurnitureItem(baseProps());
    Item CRAFTING_MATERIAL = new Item(baseProps());

    static Item.Properties baseProps() {
        return new Item.Properties();
    }

    static void registerItems(Common.RegisterHelper<Item> helper) {
        helper.register(Common.locate("artisans_workstation"), ARTISANS_WORKSTATION);
        helper.register(Common.locate("furniture"), FURNITURE);
        helper.register(Common.locate("crafting_material"), CRAFTING_MATERIAL);
    }
}
