package immersive_furniture.fabric;

import immersive_furniture.*;
import immersive_furniture.data.ServerFurnitureRegistry;
import immersive_furniture.fabric.cobalt.network.NetworkHandlerImpl;
import immersive_furniture.fabric.cobalt.registration.RegistrationImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.item.CreativeModeTabs;

public final class CommonFabric implements ModInitializer {
    static {
        new RegistrationImpl();
        new NetworkHandlerImpl();
    }

    @Override
    public void onInitialize() {
        Items.bootstrap();
        Blocks.bootstrap();
        BlockEntityTypes.bootstrap();
        Sounds.bootstrap();

        Messages.loadMessages();

        ServerPlayConnectionEvents.JOIN.register((player, handler, sender) ->
                ServerFurnitureRegistry.syncWithPlayer(player.player));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register((itemGroup) -> itemGroup.accept(Items.ARTISANS_WORKSTATION.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(Items.CRAFTING_MATERIAL.get()));
    }
}

