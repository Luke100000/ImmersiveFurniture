package immersive_furniture.forge.cobalt.network;

import immersive_furniture.Common;
import immersive_furniture.cobalt.network.Message;
import immersive_furniture.cobalt.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class NetworkHandlerImpl extends NetworkHandler.Impl {
    private static final String PROTOCOL_VERSION = "1";

    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Common.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private int id = 0;

    @Override
    synchronized public <T extends Message> void registerMessage(Class<T> msg, Function<FriendlyByteBuf, T> constructor) {
        channel.registerMessage(id++, msg,
                Message::encode,
                constructor,
                (m, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer sender = ctx.get().getSender();
                        m.receive(sender);
                    });
                    ctx.get().setPacketHandled(true);
                });
    }

    @Override
    public void sendToServer(Message m) {
        channel.sendToServer(m);
    }

    @Override
    public void sendToPlayer(Message m, ServerPlayer e) {
        channel.send(PacketDistributor.PLAYER.with(() -> e), m);
    }

}
