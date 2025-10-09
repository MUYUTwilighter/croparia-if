package cool.muyucloud.croparia.api.resource;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApiStatus.Experimental
@SuppressWarnings("unused")
public record TypeToken<T extends TypedResource<?>>(@NotNull ResourceLocation id, @NotNull T empty,
                                                    @NotNull MapCodec<T> codec) {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<TypeToken<?>> CODEC = ResourceLocation.CODEC.comapFlatMap(id -> {
        Optional<TypeToken<TypedResource<?>>> type = get(id);
        if (type.isEmpty()) {
            return DataResult.error(() -> "Undefined SpecType: %s".formatted(id));
        } else {
            return DataResult.success(type.get());
        }
    }, TypeToken::id);

    private static final Map<ResourceLocation, TypeToken<?>> REGISTRY_BY_ID = new HashMap<>();

    public static <T extends TypedResource<?>> Optional<TypeToken<T>> register(ResourceLocation id, T empty, MapCodec<T> codec) {
        if (REGISTRY_BY_ID.containsKey(id)) {
            return Optional.empty();
        }
        TypeToken<T> type = new TypeToken<>(id, empty, codec);
        REGISTRY_BY_ID.put(id, type);
        return Optional.of(type);
    }

    public static <T extends TypedResource<?>> TypeToken<T> registerOrThrow(ResourceLocation id, T empty, MapCodec<T> codec) {
        return register(id, empty, codec).orElseThrow(() -> new IllegalArgumentException("Duplicate TypeToken: %s".formatted(id)));
    }

    @SuppressWarnings("unchecked")
    public static <T extends TypedResource<?>> Optional<TypeToken<T>> get(ResourceLocation id) {
        TypeToken<?> type = REGISTRY_BY_ID.get(id);
        try {
            return Optional.ofNullable((TypeToken<T>) type);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
}
