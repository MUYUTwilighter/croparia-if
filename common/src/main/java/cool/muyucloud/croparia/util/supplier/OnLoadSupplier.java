package cool.muyucloud.croparia.util.supplier;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A lazy supplier that refreshes the flag when data pack reload completed
 */
public class OnLoadSupplier<T> extends LazySupplier<T> {
    public static long LAST_DATA_LOAD = 0L;

    public static <T> OnLoadSupplier<T> of(Supplier<T> creator) {
        return new OnLoadSupplier<>(creator);
    }

    private long lastCreate = 0L;

    public OnLoadSupplier(@NotNull Supplier<T> creator) {
        super(creator);
    }

    @Override
    public <O, M extends O> OnLoadSupplier<O> map(Function<T, M> mapper) {
        return new OnLoadSupplier<>(() -> mapper.apply(creator.get()));
    }

    @Override
    public T get() {
        if (this.getLastCreate() <= LAST_DATA_LOAD) {
            this.cache = creator.get();
            this.lastCreate = System.currentTimeMillis();
        }
        return this.cache;
    }

    public long getLastCreate() {
        return lastCreate;
    }
}
