package cool.muyucloud.croparia.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Ref<T> {
    private T value;
    private final List<BiConsumer<T, T>> onChanged = new ArrayList<>();

    public Ref(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public Ref<T> set(T value) {
        T old = this.value;
        this.value = value;
        if (!this.value.equals(old)) {
            for (BiConsumer<T, T> consumer : onChanged) {
                consumer.accept(old, value);
            }
        }
        return this;
    }

    public Ref<T> map(Function<T, T> mapper) {
        set(mapper.apply(value));
        return this;
    }

    public boolean mapAndCompare(Function<T, T> mapper) {
        T old = value;
        T newValue = mapper.apply(value);
        set(newValue);
        return newValue.equals(old);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Ref<T> onChanged(BiConsumer<T, T> consumer) {
        onChanged.add(consumer);
        return this;
    }
}
