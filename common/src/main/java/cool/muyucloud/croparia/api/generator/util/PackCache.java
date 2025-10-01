package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.generator.DataGenerator;

import java.util.*;

public class PackCache {
    private final Map<String, PackCacheEntry<?>> byPath = new HashMap<>();
    private final Map<DataGenerator, Collection<PackCacheEntry<?>>> byGenerator = new HashMap<>();

    public void clear() {
        byPath.clear();
        byGenerator.clear();
    }

    /**
     * Cache a value with the specified path and owner. If the path already exists, it will be overwritten.
     *
     * @param path  The path of the cached value.
     * @param value The value to be cached.
     * @param owner The generator owning the cached value.
     */
    public <T> T cache(String path, T value, DataGenerator owner) {
        PackCacheEntry<?> entry = byPath.get(path);
        Collection<PackCacheEntry<?>> entries = byGenerator.computeIfAbsent(entry.owner(), k -> new HashSet<>());
        entries.remove(entry);
        entry = new PackCacheEntry<>(owner, path, value);
        byPath.put(path, entry);
        entries.add(entry);
        return value;
    }

    /**
     * Safe query for cached value. If the querier is not the owner of the cached value, it will transfer the ownership to the querier.
     *
     * @param querier The generator querying the value.
     * @param path    The path of the cached value.
     * @param <T>     The type of the cached value.
     * @return An optional containing the cached value if present, otherwise an empty optional.
     * @throws ClassCastException if the cached value cannot be cast to the expected type.
     */
    public <T> Optional<T> get(DataGenerator querier, String path) throws ClassCastException {
        PackCacheEntry<?> entry = byPath.get(path);
        @SuppressWarnings("unchecked")
        T value = (T) entry.value();
        if (entry.owner() != querier) {
            cache(path, value, querier);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Safe query for all cached values owned by the specified generator.
     *
     * @param querier The generator querying the value.
     * @param <T>     The type of the cached values.
     * @return A collection of cached values owned by the specified generator.
     * @throws ClassCastException if any cached value cannot be cast to the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<PackCacheEntry<T>> getAll(DataGenerator querier) throws ClassCastException {
        return this.byGenerator.computeIfAbsent(querier, k -> new HashSet<>()).stream().map(
            entry -> new PackCacheEntry<>(entry.owner(), entry.path(), (T) entry.value())
        ).toList();
    }

    /**
     * Get all value entries.
     */
    public Collection<PackCacheEntry<?>> entries() {
        return Collections.unmodifiableCollection(byPath.values());
    }
}
