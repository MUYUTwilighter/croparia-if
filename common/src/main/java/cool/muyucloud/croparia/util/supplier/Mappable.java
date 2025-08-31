package cool.muyucloud.croparia.util.supplier;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Mappable<T> extends Supplier<T> {
    static <T, O> Mappable<O> of(Supplier<T> supplier, Function<T, O> mapper) {
        if (supplier instanceof Mappable<T> mappable) {
            return mappable.map(mapper);
        } else {
            return () -> mapper.apply(supplier.get());
        }
    }

    static <T> Mappable<T> of(Supplier<T> supplier) {
        return supplier::get;
    }

    default <S, O extends S> Mappable<S> map(Function<T, O> mapper) {
        return () -> mapper.apply(this.get());
    }

    default T get(Consumer<Throwable> onError) {
        try {
            return this.get();
        } catch (Throwable t) {
            onError.accept(t);
            return null;
        }
    }

    default T getOr(T def) {
        try {
            T v = this.get();
            return v == null ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }
}
