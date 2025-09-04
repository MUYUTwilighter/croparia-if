package cool.muyucloud.croparia.api.recipe.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record S2CSyncClear<R extends DisplayableRecipe<?>>(TypedSerializer<R> recipeType)
    implements NetworkHandler {
    public static StreamCodec<RegistryFriendlyByteBuf, S2CSyncClear<?>> STREAM_CODEC =
        CodecUtil.mapStream(TypedSerializer.CODEC, S2CSyncClear::new, S2CSyncClear::recipeType);
    public static final NetworkHandlerType<S2CSyncClear<?>> TYPE = NetworkHandlerType.ofS2C(CropariaIf.of("sync_clear"), STREAM_CODEC);

    public static <R extends DisplayableRecipe<?>> S2CSyncClear<R> of(TypedSerializer<R> type) {
        return new S2CSyncClear<>(type);
    }

    @Override
    public void handle(@Nullable NetworkManager.PacketContext context) {
        recipeType().syncClear();
    }

    @Override
    public @NotNull NetworkHandlerType<S2CSyncClear<R>> handlerType() {
        return TYPE.adapt();
    }
}
