package cool.muyucloud.croparia.api.core.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.block.entity.CropTransmuterBlockEntity;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public record CropTransmuterRedstoneModePacket(BlockPos pos) implements NetworkHandler {
    public static final NetworkHandlerType<CropTransmuterRedstoneModePacket> TYPE =
        NetworkHandlerType.ofC2S(
            CropariaIf.of("crop_transmuter_redstone_mode"),
            (buf, packet) -> packet.write(buf),
            buf -> new CropTransmuterRedstoneModePacket(buf.readBlockPos())
        );

    @Override
    public @NotNull NetworkHandlerType<?> handlerType() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CropTransmuterMenu menu)) return;
            if (!menu.getBlockPos().equals(pos)) return;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (!(be instanceof CropTransmuterBlockEntity transmuter)) return;
            transmuter.toggleRedstoneMode();
            menu.broadcastChanges();
        });
    }
}
