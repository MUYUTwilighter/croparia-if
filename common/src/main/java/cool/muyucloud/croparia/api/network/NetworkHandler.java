package cool.muyucloud.croparia.api.network;

import cool.muyucloud.croparia.CropariaIf;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public interface NetworkHandler {
    default void send() {
        FriendlyByteBuf buf = this.toPacket();
        if (this.handlerType().side() == NetworkManager.Side.C2S) {
            CropariaIf.ifClient(clientRef -> NetworkManager.sendToServer(this.handlerType().id(), buf));
            return;
        }
        CropariaIf.ifServer(server -> NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this.handlerType().id(), buf));
    }

    default void send(ServerPlayer player) {
        if (this.handlerType().side() == NetworkManager.Side.S2C) {
            CropariaIf.ifServer(server -> NetworkManager.sendToPlayer(player, this.handlerType().id(), this.toPacket()));
        }
    }

    default void send(Iterable<ServerPlayer> players) {
        if (this.handlerType().side() == NetworkManager.Side.S2C) {
            CropariaIf.ifServer(server -> NetworkManager.sendToPlayers(players, this.handlerType().id(), this.toPacket()));
        }
    }

    private FriendlyByteBuf toPacket() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        this.write(buf);
        return buf;
    }

    @NotNull NetworkHandlerType<?> handlerType();

    void write(FriendlyByteBuf buf);

    void handle(NetworkManager.PacketContext context);
}
