package cool.muyucloud.croparia.api.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.PacketTransformer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class NetworkHandlerType<H extends NetworkHandler> {
    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(@NotNull Identifier id, @NotNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, codec);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(@NotNull Identifier id,
                                                                          @NotNull StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                          @NotNull List<PacketTransformer> packetTransformers) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, codec, packetTransformers);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofC2S(@NotNull Identifier id, @NotNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.C2S, codec);
    }

    @NotNull
    private final CustomPacketPayload.Type<H> type;
    @NotNull
    private final StreamCodec<RegistryFriendlyByteBuf, H> codec;
    @NotNull
    private final NetworkManager.Side side;
    @NotNull
    private final List<PacketTransformer> packetTransformers;

    public NetworkHandlerType(@NotNull Identifier id, @NotNull NetworkManager.Side side, @NotNull StreamCodec<RegistryFriendlyByteBuf, H> codec) {
        this(id, side, codec, List.of());
    }

    public NetworkHandlerType(@NotNull Identifier id, @NotNull NetworkManager.Side side,
                              @NotNull StreamCodec<RegistryFriendlyByteBuf, H> codec,
                              @NotNull List<PacketTransformer> packetTransformers) {
        this.type = new CustomPacketPayload.Type<>(id);
        this.side = side;
        this.codec = codec;
        this.packetTransformers = List.copyOf(packetTransformers);
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

    @NotNull
    public List<PacketTransformer> packetTransformers() {
        return packetTransformers;
    }

    @SuppressWarnings("unchecked")
    public <T extends NetworkHandler> NetworkHandlerType<T> adapt() {
        return (NetworkHandlerType<T>) this;
    }
}
