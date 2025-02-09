package immersive_furniture;

import immersive_furniture.cobalt.registration.Registration;
import immersive_furniture.item.FurnitureItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public interface Items {
    List<Supplier<Item>> items = new LinkedList<>();

    Supplier<Item> ARTISANS_WORKSTATION = register("artisans_workstation", () -> new BlockItem(Blocks.ARTISANS_WORKSTATION.get(), baseProps()));
    Supplier<Item> FURNITURE = register("furniture", () -> new FurnitureItem(baseProps()));

    static Supplier<Item> register(String name, Supplier<Item> item) {
        Supplier<Item> register = Registration.register(BuiltInRegistries.ITEM, Common.locate(name), item);
        items.add(register);
        return register;
    }

    static void bootstrap() {
    }

    static Item.Properties baseProps() {
        return new Item.Properties();
    }

    static List<ItemStack> getSortedItems() {
        return items.stream().map(i -> i.get().getDefaultInstance()).toList();
    }
}
