package cool.muyucloud.croparia.api.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NetworkHandlerType<H extends NetworkHandler> {
    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(@NotNull ResourceLocation id, @NotNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, codec);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofC2S(@NotNull ResourceLocation id, @NotNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.C2S, codec);
    }

    @NotNull
    private final CustomPacketPayload.Type<H> type;
    @NotNull
    private final StreamCodec<RegistryFriendlyByteBuf, H> codec;
    @NotNull
    private final NetworkManager.Side side;

    public NetworkHandlerType(@NotNull ResourceLocation id, @NotNull NetworkManager.Side side, @NotNull StreamCodec<RegistryFriendlyByteBuf, H> codec) {
        this.type = new CustomPacketPayload.Type<>(id);
        this.side = side;
        this.codec = codec;
    }

    @NotNull
    public CustomPacketPayload.Type<H> type() {
        return this.type;
    }

    @NotNull
    public NetworkManager.Side side() {
        return side;
    }

    @NotNull
    public StreamCodec<RegistryFriendlyByteBuf, H> codec() {
        return codec;
    }

    @SuppressWarnings("unchecked")
    public <T extends NetworkHandler> NetworkHandlerType<T> adapt() {
        return (NetworkHandlerType<T>) this;
    }
}
