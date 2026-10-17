package cool.muyucloud.croparia.api.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.PacketTransformer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("unused")
public class NetworkHandlerType<H extends NetworkHandler> {
    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(@NonNull Identifier id, @NonNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, codec);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(@NonNull Identifier id,
                                                                          @NonNull StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                          @NonNull List<PacketTransformer> packetTransformers) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, codec, packetTransformers);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofC2S(@NonNull Identifier id, @NonNull StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.C2S, codec);
    }

    @NonNull
    private final CustomPacketPayload.Type<H> type;
    @NonNull
    private final StreamCodec<RegistryFriendlyByteBuf, H> codec;
    @NonNull
    private final NetworkManager.Side side;
    @NonNull
    private final List<PacketTransformer> packetTransformers;

    public NetworkHandlerType(@NonNull Identifier id, @NonNull NetworkManager.Side side, @NonNull StreamCodec<RegistryFriendlyByteBuf, H> codec) {
        this(id, side, codec, List.of());
    }

    public NetworkHandlerType(@NonNull Identifier id, @NonNull NetworkManager.Side side,
                              @NonNull StreamCodec<RegistryFriendlyByteBuf, H> codec,
                              @NonNull List<PacketTransformer> packetTransformers) {
        this.type = new CustomPacketPayload.Type<>(id);
        this.side = side;
        this.codec = codec;
        this.packetTransformers = List.copyOf(packetTransformers);
    }

    @NonNull
    public CustomPacketPayload.Type<H> type() {
        return this.type;
    }

    @NonNull
    public NetworkManager.Side side() {
        return side;
    }

    @NonNull
    public StreamCodec<RegistryFriendlyByteBuf, H> codec() {
        return codec;
    }

    @NonNull
    public List<PacketTransformer> packetTransformers() {
        return packetTransformers;
    }

    @SuppressWarnings("unchecked")
    public <T extends NetworkHandler> NetworkHandlerType<T> adapt() {
        return (NetworkHandlerType<T>) this;
    }
}
