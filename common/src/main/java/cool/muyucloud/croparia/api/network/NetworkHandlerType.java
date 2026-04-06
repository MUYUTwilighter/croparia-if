package cool.muyucloud.croparia.api.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class NetworkHandlerType<H extends NetworkHandler> {
    public static <T extends NetworkHandler> NetworkHandlerType<T> ofS2C(
        @NotNull ResourceLocation id,
        @NotNull BiConsumer<FriendlyByteBuf, T> encoder,
        @NotNull Function<FriendlyByteBuf, T> decoder
    ) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.S2C, encoder, decoder);
    }

    public static <T extends NetworkHandler> NetworkHandlerType<T> ofC2S(
        @NotNull ResourceLocation id,
        @NotNull BiConsumer<FriendlyByteBuf, T> encoder,
        @NotNull Function<FriendlyByteBuf, T> decoder
    ) {
        return new NetworkHandlerType<>(id, NetworkManager.Side.C2S, encoder, decoder);
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final BiConsumer<FriendlyByteBuf, H> encoder;
    @NotNull
    private final Function<FriendlyByteBuf, H> decoder;
    @NotNull
    private final NetworkManager.Side side;

    public NetworkHandlerType(
        @NotNull ResourceLocation id,
        @NotNull NetworkManager.Side side,
        @NotNull BiConsumer<FriendlyByteBuf, H> encoder,
        @NotNull Function<FriendlyByteBuf, H> decoder
    ) {
        this.id = id;
        this.side = side;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @NotNull
    public ResourceLocation id() {
        return this.id;
    }

    @NotNull
    public NetworkManager.Side side() {
        return side;
    }

    public void encode(FriendlyByteBuf buf, H handler) {
        this.encoder.accept(buf, handler);
    }

    @NotNull
    public H decode(FriendlyByteBuf buf) {
        return this.decoder.apply(buf);
    }

    @SuppressWarnings("unchecked")
    public <T extends NetworkHandler> NetworkHandlerType<T> adapt() {
        return (NetworkHandlerType<T>) this;
    }
}
