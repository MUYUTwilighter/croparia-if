package cool.muyucloud.croparia.api.generator.util;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface DgRegistry<E extends DgEntry> extends Iterable<E> {
    Map<Identifier, DgRegistry<?>> REGISTRY = new HashMap<>();
    Map<DgRegistry<? extends DgEntry>, Identifier> BY_INSTANCE = new HashMap<>();
    Codec<DgRegistry<? extends DgEntry>> CODEC = Identifier.CODEC.xmap(REGISTRY::get, BY_INSTANCE::get);

    static <E extends DgEntry, T extends DgRegistry<E>> T register(Identifier id, T registry) {
        REGISTRY.put(id, registry);
        BY_INSTANCE.put(registry, id);
        return registry;
    }

    static <E extends DgEntry> EnumRegistry<E> ofEnum(Class<E> enumClass) {
        return new EnumRegistry<>(enumClass);
    }

    @SuppressWarnings("unused")
    static <E extends DgEntry> DgRegistry<E> ofMap(Map<Identifier, E> map) {
        return new MapRegistry<>(map);
    }

    default Optional<E> forName(Identifier id) {
        for (E e : this) {
            if (e.getKey().equals(id)) return Optional.of(e);
        }
        return Optional.empty();
    }

    default Identifier getId() {
        return BY_INSTANCE.get(this);
    }

    class MapRegistry<E extends DgEntry> implements DgRegistry<E> {
        private final Map<Identifier, E> map;

        public MapRegistry(Map<Identifier, E> map) {
            this.map = map;
        }

        @Override
        public @NotNull Iterator<E> iterator() {
            return this.map.values().iterator();
        }

        @Override
        public Optional<E> forName(Identifier name) {
            return Optional.ofNullable(this.map.get(name));
        }
    }

    class EnumRegistry<E extends DgEntry> implements DgRegistry<E> {
        private final Map<Identifier, E> map;

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
        public Optional<E> forName(Identifier id) {
            return Optional.ofNullable(this.map.get(id));
        }
    }
}
