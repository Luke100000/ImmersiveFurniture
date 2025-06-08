package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.*;
import net.conczin.immersive_furniture.forge.cobalt.network.NetworkHandlerImpl;
import net.conczin.immersive_furniture.forge.cobalt.registration.RegistrationImpl;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod(Common.MOD_ID)
@Mod.EventBusSubscriber(modid = Common.MOD_ID, bus = Bus.MOD)
public final class CommonForge {
    static {
        new RegistrationImpl();
        new NetworkHandlerImpl();
    }

    @SuppressWarnings("unused")
    public CommonForge() {
        Items.bootstrap();
        Blocks.bootstrap();
        BlockEntityTypes.bootstrap();
        Entities.bootstrap();
        Sounds.bootstrap();

        Messages.loadMessages();
    }

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(Items.ARTISANS_WORKSTATION.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(Items.CRAFTING_MATERIAL.get());
        }
    }
}
