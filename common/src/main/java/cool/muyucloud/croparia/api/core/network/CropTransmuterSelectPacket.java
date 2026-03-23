package cool.muyucloud.croparia.api.core.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.block.entity.CropTransmuterBlockEntity;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record CropTransmuterSelectPacket(BlockPos pos, ResourceLocation selectedId) implements NetworkHandler {
    public static final StreamCodec<RegistryFriendlyByteBuf, CropTransmuterSelectPacket> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBlockPos(payload.pos);
            buf.writeResourceLocation(payload.selectedId);
        },
        buf -> new CropTransmuterSelectPacket(buf.readBlockPos(), buf.readResourceLocation())
    );
    public static final NetworkHandlerType<CropTransmuterSelectPacket> TYPE =
        NetworkHandlerType.ofC2S(CropariaIf.of("crop_transmuter_select"), STREAM_CODEC);

    @Override
    public @NotNull NetworkHandlerType<?> handlerType() {
        return TYPE;
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CropTransmuterMenu menu)) return;
            if (menu.getBlockPos() == null || !menu.getBlockPos().equals(pos)) return;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (!(be instanceof CropTransmuterBlockEntity extractor)) return;
            Material<?> material = CropTransmuterBlockEntity.materialFromInput(
                extractor.getItem(CropTransmuterBlockEntity.INPUT_SLOT)
            );
            if (material == null || !material.isTag()) return;
            Set<ResourceLocation> candidates = CropTransmuterBlockEntity.candidateItemIds(material);
            if (!candidates.contains(selectedId)) return;
            extractor.setSelectedOutput(material.getName(), selectedId);
        });
    }
}
