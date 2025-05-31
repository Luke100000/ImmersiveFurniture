package immersive_furniture.forge;

import immersive_furniture.*;
import immersive_furniture.forge.cobalt.network.NetworkHandlerImpl;
import immersive_furniture.forge.cobalt.registration.RegistrationImpl;
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
        Sounds.bootstrap();

        Messages.loadMessages();
    }
}
