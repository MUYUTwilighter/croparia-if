package cool.muyucloud.croparia.api.generator.util;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface DgRegistry<E extends DgEntry> extends Iterable<E> {
    Map<ResourceLocation, DgRegistry<?>> REGISTRY = new HashMap<>();
    Map<DgRegistry<? extends DgEntry>, ResourceLocation> BY_INSTANCE = new HashMap<>();
    Codec<DgRegistry<? extends DgEntry>> CODEC = ResourceLocation.CODEC.xmap(REGISTRY::get, BY_INSTANCE::get);

    static <E extends DgEntry, T extends DgRegistry<E>> T register(ResourceLocation id, T iterable) {
        REGISTRY.put(id, iterable);
        BY_INSTANCE.put(iterable, id);
        return iterable;
    }

    static <E extends DgEntry> DgRegistry<E> ofEnum(Class<E> enumClass) {
        return new EnumRegistry<>(enumClass);
    }

    @SuppressWarnings("unused")
    static <E extends DgEntry> DgRegistry<E> ofMap(Map<ResourceLocation, E> map) {
        return new MapRegistry<>(map);
    }

    default Optional<E> forName(ResourceLocation id) {
        for (E e : this) {
            if (e.getKey().equals(id)) return Optional.of(e);
        }
        return Optional.empty();
    }

    default ResourceLocation getId() {
        return BY_INSTANCE.get(this);
    }

    class MapRegistry<E extends DgEntry> implements DgRegistry<E> {
        private final Map<ResourceLocation, E> map;

        public MapRegistry(Map<ResourceLocation, E> map) {
            this.map = map;
        }

        @Override
        public @NotNull Iterator<E> iterator() {
            return this.map.values().iterator();
        }

        @Override
        public Optional<E> forName(ResourceLocation name) {
            return Optional.ofNullable(this.map.get(name));
        }
    }

    class EnumRegistry<E extends DgEntry> implements DgRegistry<E> {
        private final Map<ResourceLocation, E> map;

        public EnumRegistry(Class<E> enumClass) {
            this.map = new LinkedHashMap<>();
            for (E e : enumClass.getEnumConstants()) {
                this.map.put(e.getKey(), e);
            }
        }

        @Override
        public @NotNull Iterator<E> iterator() {
            return this.map.values().iterator();
        }

        @Override
        public Optional<E> forName(ResourceLocation id) {
            return Optional.ofNullable(this.map.get(id));
        }
    }
}
