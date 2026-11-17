package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.generator.DataGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public record PackCacheEntry<T>(@NotNull DataGenerator owner, @NotNull String path, T value) {
    public Optional<T> getCache() {
        return Optional.ofNullable(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PackCacheEntry<?> that)) return false;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
