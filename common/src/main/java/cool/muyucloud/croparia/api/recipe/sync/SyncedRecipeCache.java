package cool.muyucloud.croparia.api.recipe.sync;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.RecipeManagerAccess;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncRecipeChunk;
import cool.muyucloud.croparia.registry.Recipes;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SyncedRecipeCache {
    private static final int TARGET_CHUNK_BYTES = 256 * 1024;

    private static final Map<Identifier, List<RecipeHolder<?>>> LIVE = new ConcurrentHashMap<>();
    private static final Object CLIENT_LOCK = new Object();
    @Nullable
    private static UUID clientSyncId;

    private SyncedRecipeCache() {}

    public static boolean usesSyncedCache(TypedSerializer<?> serializer) {
        return serializer.isClientSynchronized();
    }

    @SuppressWarnings("unchecked")
    public static <R extends DisplayableRecipe<?>> List<RecipeHolder<R>> getSyncedHolders(TypedSerializer<R> serializer) {
        List<RecipeHolder<?>> holders = LIVE.get(serializer.getId());
        if (holders == null) {
            return List.of();
        }
        return holders.stream().map(holder -> (RecipeHolder<R>) holder).toList();
    }

    public static boolean hasSnapshot(TypedSerializer<?> serializer) {
        return LIVE.containsKey(serializer.getId());
    }

    public static void clearClient() {
        synchronized (CLIENT_LOCK) {
            LIVE.clear();
            clientSyncId = null;
        }
        CompatRecipeRefresh.clear();
    }

    public static void acceptChunk(UUID syncId, Identifier recipeType, int index, int total, List<RecipeHolder<?>> recipes) {
        boolean reset = false;
        synchronized (CLIENT_LOCK) {
            if (!syncId.equals(clientSyncId)) {
                LIVE.clear();
                clientSyncId = syncId;
                reset = true;
            }
            LIVE.put(recipeType, List.copyOf(recipes));
        }
        if (reset) {
            CropariaIf.LOGGER.info("Croparia synced recipes: begin client sync {}", syncId);
        }
        CropariaIf.LOGGER.info(
            "Croparia synced recipes: received chunk {}/{} for {} with {} recipes in sync {}",
            index + 1,
            total,
            recipeType,
            recipes.size(),
            syncId
        );
        CompatRecipeRefresh.onRecipesUpdated(java.util.Set.of(recipeType));
    }

    public static void syncAll(MinecraftServer server) {
        syncPlayers(server, server.getPlayerList().getPlayers());
    }

    public static void syncPlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        syncPlayers(server, List.of(player));
    }

    private static void syncPlayers(MinecraftServer server, Iterable<ServerPlayer> players) {
        List<TypedSerializer<?>> serializers = getSynchronizedTypes();
        if (serializers.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            if (!NetworkManager.canPlayerReceive(player, S2CSyncRecipeChunk.TYPE.type())) {
                continue;
            }

            UUID syncId = UUID.randomUUID();
            for (TypedSerializer<?> serializer : serializers) {
                sendChunks(server, player, syncId, serializer);
            }
        }
    }

    private static List<TypedSerializer<?>> getSynchronizedTypes() {
        List<TypedSerializer<?>> serializers = new ArrayList<>();
        Recipes.forEach(serializer -> {
            if (serializer.isClientSynchronized()) {
                serializers.add(serializer);
            }
        });
        serializers.sort(Comparator.comparing(serializer -> serializer.getId().toString()));
        return serializers;
    }

    private static <R extends DisplayableRecipe<?>> void sendChunks(MinecraftServer server, ServerPlayer player, UUID syncId, TypedSerializer<R> serializer) {
        List<RecipeHolder<?>> holders = ((RecipeManagerAccess) server.getRecipeManager()).cif$byType(serializer.adapt()).stream()
            .filter(serializer::shouldSync)
            .<RecipeHolder<?>>map(holder -> holder)
            .toList();

        List<List<RecipeHolder<?>>> chunks = chunkBySize(player, holders);
        if (chunks.isEmpty()) {
            chunks = List.of(List.of());
        }

        for (int index = 0; index < chunks.size(); index++) {
            S2CSyncRecipeChunk.of(syncId, serializer.getId(), index, chunks.size(), chunks.get(index)).send(player);
        }
    }

    private static List<List<RecipeHolder<?>>> chunkBySize(ServerPlayer player, List<RecipeHolder<?>> recipes) {
        if (recipes.isEmpty()) {
            return List.of();
        }

        List<List<RecipeHolder<?>>> chunks = new ArrayList<>();
        List<RecipeHolder<?>> current = new ArrayList<>();
        int currentBytes = 0;

        for (RecipeHolder<?> recipe : recipes) {
            int recipeBytes = measureRecipe(player, recipe);
            if (!current.isEmpty() && currentBytes + recipeBytes > TARGET_CHUNK_BYTES) {
                chunks.add(List.copyOf(current));
                current.clear();
                currentBytes = 0;
            }
            current.add(recipe);
            currentBytes += recipeBytes;
        }

        if (!current.isEmpty()) {
            chunks.add(List.copyOf(current));
        }

        return chunks;
    }

    private static int measureRecipe(ServerPlayer player, RecipeHolder<?> recipe) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            RecipeHolder.STREAM_CODEC.encode(buffer, recipe);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }
}
