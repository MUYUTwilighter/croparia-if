package cool.muyucloud.croparia.api.network;

import cool.muyucloud.croparia.CropariaIf;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public interface NetworkHandler extends CustomPacketPayload {
    @Override
    default @NotNull Type<? extends CustomPacketPayload> type() {
        return this.handlerType().type();
    }

    default void send() {
        if (this.handlerType().side() == NetworkManager.Side.C2S) {
            CropariaIf.ifClient(clientRef -> NetworkManager.sendToServer(this));
            return;
        }
        CropariaIf.ifServer(server -> NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this));
    }

    default void send(ServerPlayer player) {
        if (this.handlerType().side() == NetworkManager.Side.S2C) {
            CropariaIf.ifServer(server -> NetworkManager.sendToPlayer(player, this));
        }
    }

    @SuppressWarnings("unused")
    default void send(Iterable<ServerPlayer> players) {
        if (this.handlerType().side() == NetworkManager.Side.S2C) {
            CropariaIf.ifServer(server -> NetworkManager.sendToPlayers(players, this));
        }
    }

    @NotNull NetworkHandlerType<?> handlerType();

    void handle(NetworkManager.PacketContext context);
}
