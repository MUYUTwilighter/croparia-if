package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NonNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public interface MapReader<K, V> extends Iterable<Map.Entry<K, V>> {
    static <K, V> MapReader<K, V> map(@NonNull Map<K, V> map) {
        return new MapReaderImpl<>(map);
    }

    static JsonObjectReader json(@NonNull JsonObject object) {
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
    default @NonNull Iterator<Map.Entry<K, V>> iterator() {
        return entries().iterator();
    }

    default <K2, V2> Mapped<K, V, K2, V2> map(@NonNull Function<K, K2> keyMapper, @NonNull Function<K2, V2> getter) {
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

    record Mapped<KF, VF, K, V>(@NonNull MapReader<KF, VF> from,
                                @NonNull Function<KF, K> keyMapper,
                                @NonNull Function<K, V> getter) implements MapReader<K, V> {
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
        public @NonNull Iterator<Map.Entry<K, V>> iterator() {
            return this.from().entries().stream().map(e -> (Map.Entry<K, V>) new AbstractMap.SimpleEntry<>(
                keyMapper.apply(e.getKey()), this.get(keyMapper().apply(e.getKey()))
            )).iterator();
        }
    }

    class MapReaderImpl<K, V> implements MapReader<K, V> {
        @NonNull
        private final Map<K, V> map;

        public MapReaderImpl(@NonNull Map<K, V> map) {
            this.map = map;
        }

        public @NonNull Map<K, V> get() {
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
