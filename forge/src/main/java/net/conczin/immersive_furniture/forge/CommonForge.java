package net.conczin.immersive_furniture.forge;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.Sounds;
import net.conczin.immersive_furniture.block.Blocks;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Consumer;
import java.util.function.Function;

@Mod(Common.MOD_ID)
@Mod.EventBusSubscriber(modid = Common.MOD_ID, bus = Bus.MOD)
public final class CommonForge {
    private static <T> void registerHelper(RegisterEvent event, Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        event.register(
                register.key(),
                registry -> consumer.accept(registry::register)
        );
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        registerHelper(event, BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(event, BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(event, BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);
        registerHelper(event, BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);

        if (event.getRegistryKey() == Registries.BLOCK_ENTITY_TYPE) {
            event.register(Registries.BLOCK_ENTITY_TYPE, helper ->
                    BlockEntityTypes.register((name, factory, block) -> {
                        //noinspection DataFlowIssue
                        BlockEntityType<?> build = BlockEntityType.Builder.of(factory::create, block).build(null);
                        helper.register(name, build);
                        return build;
                    }));
        }
    }

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Common.locate("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int id = 0;


    static class ForgeRegistrar implements Network.Registrar {
        @Override
        public <T extends ImmersivePayload> void register(Class<T> msg, Function<FriendlyByteBuf, T> constructor) {
            INSTANCE.registerMessage(
                    id++,
                    msg,
                    ImmersivePayload::encode,
                    constructor,
                    (m, ctx) -> {
                        ctx.get().enqueueWork(() -> m.handle(ctx.get().getSender()));
                        ctx.get().setPacketHandled(true);
                    }
            );
        }
    }

    static {
        Network.register(new ForgeRegistrar());
        Network.registerSender((payload, player) -> INSTANCE.sendTo(payload, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT));
        Network.registerClientSender(INSTANCE::sendToServer);
    }

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(Items.ARTISANS_WORKSTATION);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(Items.CRAFTING_MATERIAL);
        }
    }
}
