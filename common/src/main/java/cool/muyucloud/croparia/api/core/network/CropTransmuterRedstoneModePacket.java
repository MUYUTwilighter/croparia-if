package cool.muyucloud.croparia.api.core.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.block.entity.CropTransmuterBlockEntity;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public record CropTransmuterRedstoneModePacket(BlockPos pos) implements NetworkHandler {
    public static final StreamCodec<RegistryFriendlyByteBuf, CropTransmuterRedstoneModePacket> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBlockPos(payload.pos),
        buf -> new CropTransmuterRedstoneModePacket(buf.readBlockPos())
    );
    public static final NetworkHandlerType<CropTransmuterRedstoneModePacket> TYPE =
        NetworkHandlerType.ofC2S(CropariaIf.of("crop_transmuter_redstone_mode"), STREAM_CODEC);

    @Override
    public @NotNull NetworkHandlerType<?> handlerType() {
        return TYPE;
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
