package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.network.c2s.CraftRequest;
import net.conczin.immersive_furniture.network.c2s.FurnitureDataRequest;
import net.conczin.immersive_furniture.network.c2s.CraftRequest;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.conczin.immersive_furniture.network.s2c.FurnitureRegistryMessage;
import net.conczin.immersive_furniture.network.s2c.PoseOffsetMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class Network {
    private static Sender sender;
    private static ClientSender clientSender;

    public static void registerSender(Sender sender) {
        Network.sender = sender;
    }

    public static void registerClientSender(ClientSender clientSender) {
        Network.clientSender = clientSender;
    }

    public static void sendToServer(ImmersivePayload payload) {
        clientSender.sendToServer(payload);
    }

    public static void sendToPlayer(ImmersivePayload payload, ServerPlayer player) {
        sender.sendToPlayer(player, payload);
    }

    public static void sendToAllPlayers(MinecraftServer server, ImmersivePayload payload) {
        server.getPlayerList().getPlayers().forEach(p -> sendToPlayer(payload, p));
    }

    public interface Sender {
        void sendToPlayer(ServerPlayer player, ImmersivePayload payload);
    }

    public interface ClientSender {
        void sendToServer(ImmersivePayload payload);
    }

    public static void register(Registrar c) {
        c.register(CraftRequest.TYPE, CraftRequest.STREAM_CODEC, true);
        c.register(FurnitureDataRequest.TYPE, FurnitureDataRequest.STREAM_CODEC, true);

        c.register(FurnitureDataResponse.TYPE, FurnitureDataResponse.STREAM_CODEC, false);
        c.register(FurnitureRegistryMessage.TYPE, FurnitureRegistryMessage.STREAM_CODEC, false);
        c.register(PoseOffsetMessage.TYPE, PoseOffsetMessage.STREAM_CODEC, false);
    }

    public interface Registrar {
        <T extends ImmersivePayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec, boolean isServer);
    }
}
