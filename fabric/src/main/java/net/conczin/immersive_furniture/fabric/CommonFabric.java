package net.conczin.immersive_furniture.fabric;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.recipe.Recipes;
import net.conczin.immersive_furniture.Sounds;
import net.conczin.immersive_furniture.block.Blocks;
import net.conczin.immersive_furniture.block.entity.BlockEntityTypes;
import net.conczin.immersive_furniture.data.FurnitureDataManager;
import net.conczin.immersive_furniture.data.ServerFurnitureRegistry;
import net.conczin.immersive_furniture.entity.Entities;
import net.conczin.immersive_furniture.item.Items;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.conczin.immersive_furniture.network.Network;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;

import java.util.function.Consumer;

public final class CommonFabric implements ModInitializer {
    private static <T> void registerHelper(Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        consumer.accept((name, value) -> Registry.register(register, name, value));
    }

    Network.Registrar fabricRegistrar = new Network.Registrar() {
        @Override
        public <T extends ImmersivePayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, boolean isServer) {
            if (isServer) {
                PayloadTypeRegistry.playC2S().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> ctx.server().execute(() -> payload.handle(ctx.player())));
            } else {
                PayloadTypeRegistry.playS2C().register(type, codec);
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                    ClientProxy.register(type);
                }
            }
        }
    };

    @Override
    public void onInitialize() {
        registerHelper(BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);
        registerHelper(BuiltInRegistries.ENTITY_TYPE, Entities::registerEntities);
        registerHelper(BuiltInRegistries.RECIPE_SERIALIZER, Recipes::registerRecipes);

        BlockEntityTypes.register((name, factory, block) ->
                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, BlockEntityType.Builder.of(factory::create, block).build(null)));

        Network.register(fabricRegistrar);
        Network.registerSender(ServerPlayNetworking::send);

        ServerPlayConnectionEvents.JOIN.register((player, handler, sender) ->
                ServerFurnitureRegistry.syncWithPlayer(player.player));

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> FurnitureDataManager.setWorldRoot(server.getWorldPath(LevelResource.ROOT)));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register((itemGroup) -> itemGroup.accept(Items.ARTISANS_WORKSTATION));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(Items.CRAFTING_MATERIAL));
    }

    private static final class ClientProxy {
        public static <T extends ImmersivePayload> void register(ImmersivePayload.Type<T> type) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> ctx.client().execute(() -> payload.handle(ctx.player())));
        }
    }
}

