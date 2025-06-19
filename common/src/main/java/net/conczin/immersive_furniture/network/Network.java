package net.conczin.immersive_furniture.network;

import net.conczin.immersive_furniture.network.c2s.FurnitureDataRequest;
import net.conczin.immersive_furniture.network.s2c.CraftRequest;
import net.conczin.immersive_furniture.network.s2c.FurnitureDataResponse;
import net.conczin.immersive_furniture.network.s2c.FurnitureRegistryMessage;
import net.conczin.immersive_furniture.network.s2c.PoseOffsetMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

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
        sender.sendToPlayer(payload, player);
    }

    public static void sendToAllPlayers(MinecraftServer server, ImmersivePayload payload) {
        server.getPlayerList().getPlayers().forEach(p -> sendToPlayer(payload, p));
    }

    public static void register(Registrar c) {
        c.register(FurnitureDataRequest.class, FurnitureDataRequest::new);

        c.register(CraftRequest.class, CraftRequest::new);
        c.register(FurnitureDataResponse.class, FurnitureDataResponse::new);
        c.register(FurnitureRegistryMessage.class, FurnitureRegistryMessage::new);
        c.register(PoseOffsetMessage.class, PoseOffsetMessage::new);
    }

    public interface Registrar {
        <T extends ImmersivePayload> void register(Class<T> msg, Function<FriendlyByteBuf, T> constructor);
    }

    public interface Sender {
        void sendToPlayer(ImmersivePayload payload, ServerPlayer player);
    }

    public interface ClientSender {
        void sendToServer(ImmersivePayload payload);
    }
}
