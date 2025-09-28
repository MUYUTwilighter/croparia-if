package cool.muyucloud.croparia.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public interface MapReader<K, V> extends Iterable<Map.Entry<K, V>> {
    static <K, V> MapReaderImpl<K, V> map(@NotNull Map<K, V> map) {
        return new MapReaderImpl<>(map);
    }

    static JsonObjectReader jsonObject(@NotNull JsonObject object) {
        return new JsonObjectReader(object);
    }

    V get(K key);

    Collection<Map.Entry<K, V>> entries();

    @SuppressWarnings("unused")
    default Collection<K> keys() {
        return entries().stream().map(Map.Entry::getKey).toList();
    }

    default Collection<V> values() {
        return entries().stream().map(Map.Entry::getValue).toList();
    }

    default int size() {
        return entries().size();
    }

    @Override
    default @NotNull Iterator<Map.Entry<K, V>> iterator() {
        return entries().iterator();
    }

    default <K2, V2> Mapped<K, V, K2, V2> map(@NotNull Function<K, K2> keyMapper, @NotNull Function<K2, V2> getter) {
        return new Mapped<>(this, keyMapper, getter);
    }
    
    class JsonObjectReader implements MapReader<String, JsonElement> {
        private final JsonObject object;

        public JsonObjectReader(JsonObject object) {
            this.object = object;
        }

        public JsonObject get() {
            return object;
        }

        @Override
        public JsonElement get(String key) {
            return this.get().get(key);
        }

        @Override
        public Collection<Map.Entry<String, JsonElement>> entries() {
            return this.get().entrySet();
        }

        @Override
        public Collection<String> keys() {
            return this.get().keySet();
        }

        @Override
        public Collection<JsonElement> values() {
            return this.get().asMap().values();
        }
    }

    record Mapped<KF, VF, K, V>(@NotNull MapReader<KF, VF> from,
                                @NotNull Function<KF, K> keyMapper,
                                @NotNull Function<K, V> getter) implements MapReader<K, V> {
        @Override
        public V get(K key) {
            return getter().apply(key);
        }

        @Override
        public int size() {
            return this.from().size();
        }

        @Override
        public Collection<Map.Entry<K, V>> entries() {
            return this.from().entries().stream().map(e -> (Map.Entry<K, V>) new AbstractMap.SimpleEntry<>(
                keyMapper.apply(e.getKey()), this.get(keyMapper().apply(e.getKey()))
            )).toList();
        }

        @Override
        public Collection<K> keys() {
            return this.entries().stream().map(Map.Entry::getKey).toList();
        }

        @Override
        public Collection<V> values() {
            return this.entries().stream().map(Map.Entry::getValue).toList();
        }

        @Override
        public @NotNull Iterator<Map.Entry<K, V>> iterator() {
            return this.from().entries().stream().map(e -> (Map.Entry<K, V>) new AbstractMap.SimpleEntry<>(
                keyMapper.apply(e.getKey()), this.get(keyMapper().apply(e.getKey()))
            )).iterator();
        }
    }

    class MapReaderImpl<K, V> implements MapReader<K, V> {
        @NotNull
        private final Map<K, V> map;

        public MapReaderImpl(@NotNull Map<K, V> map) {
            this.map = map;
        }

        public @NotNull Map<K, V> get() {
            return map;
        }

        @Override
        public V get(K key) {
            return this.get().get(key);
        }

        @Override
        public Collection<Map.Entry<K, V>> entries() {
            return this.get().entrySet();
        }

        @Override
        public Collection<K> keys() {
            return this.get().keySet();
        }

        @Override
        public Collection<V> values() {
            return this.get().values();
        }
    }
}
