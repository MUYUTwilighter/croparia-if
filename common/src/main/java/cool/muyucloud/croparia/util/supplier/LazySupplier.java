package cool.muyucloud.croparia.util.supplier;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Supplier that only create the value when it is called via {@link #get()} for the first time.
 */
public class LazySupplier<T> implements Mappable<T> {
    public static <T> LazySupplier<T> empty() {
        return LazySupplier.of(() -> null);
    }

    public static <T> LazySupplier<T> of(Supplier<T> creator) {
        return creator.getClass() == LazySupplier.class ? (LazySupplier<T>) creator : new LazySupplier<>(creator);
    }

    protected Supplier<T> creator;
    protected T cache;
    protected boolean loaded = false;

    public LazySupplier(Supplier<T> creator) {
        this.creator = creator;
    }

    @Override
    public <O, M extends O> LazySupplier<O> map(Function<T, M> mapper) {
        return new LazySupplier<>(() -> mapper.apply(this.get()));
    }

    @SuppressWarnings("unused")
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public T get() {
        if (!loaded) {
            cache = creator.get();
            loaded = true;
        }
        return cache;
    }
}
