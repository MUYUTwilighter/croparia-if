package cool.muyucloud.croparia.api.recipe.network;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.sync.SyncedRecipeCache;
import cool.muyucloud.croparia.registry.Recipes;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record S2CSyncRecipeChunk(UUID syncId, Identifier recipeType, int chunkIndex, int chunkCount,
                                 List<RecipeHolder<?>> recipes) implements NetworkHandler {
    private static final SplitPacketTransformer SPLITTER = new SplitPacketTransformer();
    private static final StreamCodec<ByteBuf, ResourceKey<Recipe<?>>> RECIPE_KEY_STREAM_CODEC =
        ResourceKey.streamCodec(Registries.RECIPE);

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncRecipeChunk> STREAM_CODEC = StreamCodec.of(
        S2CSyncRecipeChunk::encode,
        S2CSyncRecipeChunk::decode
    );
    public static final NetworkHandlerType<S2CSyncRecipeChunk> TYPE = NetworkHandlerType.ofS2C(
        CropariaIf.of("sync_recipe_chunk"),
        STREAM_CODEC,
        List.of(SPLITTER)
    );

    public static S2CSyncRecipeChunk of(UUID syncId, Identifier recipeType, int chunkIndex, int chunkCount,
                                        List<RecipeHolder<?>> recipes) {
        return new S2CSyncRecipeChunk(syncId, recipeType, chunkIndex, chunkCount, List.copyOf(recipes));
    }

    @Override
    public @NotNull NetworkHandlerType<?> handlerType() {
        return TYPE;
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> SyncedRecipeCache.acceptChunk(syncId, recipeType, chunkIndex, chunkCount, recipes));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, S2CSyncRecipeChunk payload) {
        UUIDUtil.STREAM_CODEC.encode(buffer, payload.syncId());
        Identifier.STREAM_CODEC.encode(buffer, payload.recipeType());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chunkIndex());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chunkCount());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.recipes().size());

        TypedSerializer<?> serializer = getSerializer(payload.recipeType());
        for (RecipeHolder<?> holder : payload.recipes()) {
            RECIPE_KEY_STREAM_CODEC.encode(buffer, holder.id());
            encodeRecipe(buffer, serializer, holder);
        }
    }

    private static S2CSyncRecipeChunk decode(RegistryFriendlyByteBuf buffer) {
        UUID syncId = UUIDUtil.STREAM_CODEC.decode(buffer);
        Identifier recipeType = Identifier.STREAM_CODEC.decode(buffer);
        int chunkIndex = ByteBufCodecs.VAR_INT.decode(buffer);
        int chunkCount = ByteBufCodecs.VAR_INT.decode(buffer);
        int size = ByteBufCodecs.VAR_INT.decode(buffer);

        TypedSerializer<?> serializer = getSerializer(recipeType);
        List<RecipeHolder<?>> recipes = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ResourceKey<Recipe<?>> key = RECIPE_KEY_STREAM_CODEC.decode(buffer);
            recipes.add(decodeRecipe(buffer, serializer, key));
        }
        return new S2CSyncRecipeChunk(syncId, recipeType, chunkIndex, chunkCount, List.copyOf(recipes));
    }

    private static TypedSerializer<?> getSerializer(Identifier recipeType) {
        TypedSerializer<?> serializer = Recipes.find(recipeType);
        if (serializer == null) {
            throw new IllegalStateException("Unknown synced recipe type: " + recipeType);
        }
        return serializer;
    }

    @SuppressWarnings("unchecked")
    private static void encodeRecipe(RegistryFriendlyByteBuf buffer, TypedSerializer<?> serializer, RecipeHolder<?> holder) {
        ((TypedSerializer<DisplayableRecipe<?>>) serializer).streamCodec().encode(buffer, (DisplayableRecipe<?>) holder.value());
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<?> decodeRecipe(RegistryFriendlyByteBuf buffer, TypedSerializer<?> serializer, ResourceKey<Recipe<?>> key) {
        DisplayableRecipe<?> recipe = ((TypedSerializer<DisplayableRecipe<?>>) serializer).streamCodec().decode(buffer);
        return new RecipeHolder<>(key, recipe);
    }
}
