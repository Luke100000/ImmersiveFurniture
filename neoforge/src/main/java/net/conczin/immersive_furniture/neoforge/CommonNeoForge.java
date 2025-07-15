package net.conczin.immersive_furniture.neoforge;

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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Consumer;

@Mod(Common.MOD_ID)
@EventBusSubscriber(modid = Common.MOD_ID)
public final class CommonNeoForge {
    private static <T> void registerHelper(RegisterEvent event, Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        event.register(register.key(), registry -> consumer.accept(registry::register));
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

    static class NeoForgeRegistrar implements Network.Registrar {
        PayloadRegistrar registrar;

        public NeoForgeRegistrar(PayloadRegistrar registrar) {
            this.registrar = registrar;
        }

        @Override
        public <T extends ImmersivePayload> void register(ImmersivePayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, boolean isServer) {
            if (isServer) {
                registrar.playToServer(type, codec, (payload, ctx) -> ctx.enqueueWork(() -> payload.handle(ctx.player())));
            } else {
                registrar.playToClient(type, codec, (payload, ctx) -> ctx.enqueueWork(() -> payload.handle(ctx.player())));
            }
        }
    }

    @SubscribeEvent
    public static void registerNetwork(final RegisterPayloadHandlersEvent event) {
        Network.register(new NeoForgeRegistrar(event.registrar("1")));
        Network.registerSender(PacketDistributor::sendToPlayer);
        Network.registerClientSender(PacketDistributor::sendToServer);
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
