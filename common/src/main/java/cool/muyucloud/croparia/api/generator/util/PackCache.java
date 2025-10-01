package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.generator.DataGenerator;

import java.util.*;

public class PackCache {
    private final Map<String, PackCacheEntry<?>> byPath = new HashMap<>();
    private final Map<DataGenerator, Set<PackCacheEntry<?>>> byGenerator = new HashMap<>();

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
        if (entry != null) {
            byGenerator.computeIfAbsent(entry.owner(), k -> new HashSet<>()).remove(entry);
        }
        entry = new PackCacheEntry<>(owner, path, value);
        byGenerator.computeIfAbsent(owner, k -> new HashSet<>()).add(entry);
        byPath.put(path, entry);
        return value;
    }

    /**
     * Safe query for cached value. If the querier is not the owner of the cached value, this will transfer the ownership to the querier.
     *
     * @param querier The generator querying the value.
     * @param path    The path of the cached value.
     * @param <T>     The type of the cached value.
     * @return An optional containing the cached value if present, otherwise an empty optional.
     * @throws ClassCastException if the cached value cannot be cast to the expected type.
     */
    public <T> Optional<T> occupy(DataGenerator querier, String path) throws ClassCastException {
        PackCacheEntry<?> entry = byPath.get(path);
        if (entry == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T value = (T) entry.value();
        if (entry.owner() != querier) {
            cache(path, value, querier);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Similar to {@link #occupy(DataGenerator, String)}, but will not transfer the ownership.
     *
     * @return An optional containing the cached value if present and owned by the querier, otherwise an empty optional.
     */
    public <T> Optional<T> query(DataGenerator querier, String path) throws ClassCastException {
        PackCacheEntry<?> entry = byPath.get(path);
        if (entry == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T value = (T) entry.value();
        if (entry.owner() != querier) {
            return Optional.empty();    // Not owned by the querier, reject
        }
        return Optional.ofNullable(value);
    }

    /**
     * Safe query for all cached values owned by the specified generator.
     *
     * @param querier The generator querying the value.
     * @return A collection of cached values owned by the specified generator.
     * @throws ClassCastException if any cached value cannot be cast to the expected type.
     */
    public Set<PackCacheEntry<?>> getAll(DataGenerator querier) {
        return this.byGenerator.computeIfAbsent(querier, k -> new HashSet<>());
    }

    /**
     * Get all value entries.
     */
    public Collection<PackCacheEntry<?>> entries() {
        return Collections.unmodifiableCollection(byPath.values());
    }
}
