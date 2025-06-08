package net.conczin.immersive_furniture.fabric;

import io.netty.buffer.Unpooled;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.Sounds;
import net.conczin.immersive_furniture.block.Blocks;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CommonFabric implements ModInitializer {
    private static <T> void registerHelper(Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        consumer.accept((name, value) -> Registry.register(register, name, value));
    }

    FabricRegistrar fabricRegistrar = new FabricRegistrar();

    public static class FabricRegistrar implements Network.Registrar {
        private final Map<Class<?>, ResourceLocation> identifiers = new HashMap<>();
        private int id = 0;

        private <T> ResourceLocation createMessageIdentifier(Class<T> msg) {
            return new ResourceLocation(Common.MOD_ID, msg.getSimpleName().toLowerCase(Locale.ROOT).substring(0, 8) + id++);
        }

        private ResourceLocation getMessageIdentifier(ImmersivePayload msg) {
            return Objects.requireNonNull(identifiers.get(msg.getClass()), "Used unregistered message!");
        }

        @Override
        public <T extends ImmersivePayload> void register(Class<T> msg, Function<FriendlyByteBuf, T> constructor) {
            ResourceLocation identifier = createMessageIdentifier(msg);
            identifiers.put(msg, identifier);

            ServerPlayNetworking.registerGlobalReceiver(identifier, (server, player, handler, buffer, responder) -> {
                ImmersivePayload m = constructor.apply(buffer);
                server.execute(() -> m.handle(player));
            });

            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                ClientProxy.register(identifier, constructor);
            }
        }
    }

    @Override
    public void onInitialize() {
        registerHelper(BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);
        registerHelper(BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);

        //noinspection DataFlowIssue
        BlockEntityTypes.register((name, factory, block) ->
                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, BlockEntityType.Builder.of(factory::create, block).build(null)));

        Network.register(fabricRegistrar);
        Network.registerSender((payload, player) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            payload.encode(buf);
            ServerPlayNetworking.send(player, fabricRegistrar.getMessageIdentifier(payload), buf);
        });
        Network.registerClientSender((payload) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            payload.encode(buf);
            ClientPlayNetworking.send(fabricRegistrar.getMessageIdentifier(payload), buf);
        });

        ServerPlayConnectionEvents.JOIN.register((player, handler, sender) ->
                ServerFurnitureRegistry.syncWithPlayer(player.player));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register((itemGroup) -> itemGroup.accept(Items.ARTISANS_WORKSTATION));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(Items.CRAFTING_MATERIAL));
    }

    private static final class ClientProxy {
        public static <T extends ImmersivePayload> void register(ResourceLocation id, Function<FriendlyByteBuf, T> constructor) {
            ClientPlayNetworking.registerGlobalReceiver(id, (client, ignore1, buffer, ignore2) -> {
                ImmersivePayload m = constructor.apply(buffer);
                client.execute(() -> m.handle(client.player));
            });
        }
    }
}

