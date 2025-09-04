package cool.muyucloud.croparia.api.recipe.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

public record S2CSyncRecipe<R extends DisplayableRecipe<?>>(TypedSerializer<R> recipeType, R recipe) implements NetworkHandler {
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncRecipe<?>> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeJsonWithCodec(TypedSerializer.CODEC, packet.recipeType());
            buf.writeJsonWithCodec(packet.recipeType().adapt().codec().codec(), packet.recipe().adapt());
        },
        buf -> {
            TypedSerializer<?> type = buf.readJsonWithCodec(TypedSerializer.CODEC);
            DisplayableRecipe<?> recipe = buf.readJsonWithCodec(type.codec().codec());
            return new S2CSyncRecipe<>(type.adapt(), recipe);
        }
    );
    public static final NetworkHandlerType<S2CSyncRecipe<?>> TYPE = NetworkHandlerType.ofS2C(CropariaIf.of("sync_recipe"), STREAM_CODEC);

    public static <I extends RecipeInput, R extends DisplayableRecipe<I>> S2CSyncRecipe<R> of(R recipe) {
        return new S2CSyncRecipe<>(recipe.getTypedSerializer().adapt(), recipe);
    }

    @Override
    public @NotNull NetworkHandlerType<S2CSyncRecipe<R>> handlerType() {
        return TYPE.adapt();
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        recipeType().recordRecipe(recipe());
    }
}
