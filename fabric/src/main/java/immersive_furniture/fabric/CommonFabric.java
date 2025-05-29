package immersive_furniture.fabric;

import immersive_furniture.*;
import immersive_furniture.data.ServerFurnitureRegistry;
import immersive_furniture.fabric.cobalt.network.NetworkHandlerImpl;
import immersive_furniture.fabric.cobalt.registration.RegistrationImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

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

        CreativeModeTab group = FabricItemGroup.builder()
                .title(ItemGroups.getDisplayName())
                .icon(ItemGroups::getIcon)
                .displayItems((enabledFeatures, entries) -> entries.acceptAll(Items.getSortedItems()))
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Common.locate("group"), group);
    }
}

