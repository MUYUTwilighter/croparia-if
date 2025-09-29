package cool.muyucloud.croparia.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MultiFieldCodec<T> extends MapCodec<T> implements Iterable<Map.Entry<String, TestedCodec<? extends T>>> {
    private final Map<String, TestedCodec<? extends T>> codecs = new LinkedHashMap<>();

    public MultiFieldCodec(Map<String, TestedCodec<? extends T>> codecs) {
        this.codecs.putAll(codecs);
    }

    @Override
    public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
        return this.codecs.keySet().stream().map(ops::createString);
    }

    @Override
    public <I> DataResult<T> decode(DynamicOps<I> ops, MapLike<I> input) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.codecs.size());
        for (var entry : this.codecs.entrySet()) {
            String key = entry.getKey();
            TestedCodec<T> adapted = entry.getValue().adapt();
            DataResult<Pair<T, I>> result = adapted.decode(ops, input.get(key));
            if (result.isSuccess()) {
                return result.map(Pair::getFirst);
            } else {
                result.ifError(error -> logs.add(error.messageSupplier()));
            }
        }
        return DataResult.error(() -> MultiCodec.buildMsg(logs));
    }

    @Override
    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.codecs.size());
        for (var entry : this.codecs.entrySet()) {
            String key = entry.getKey();
            TestedCodec<T> adapted = entry.getValue().adapt();
            DataResult<O> result = adapted.encodeStart(ops, input);
            if (result.isSuccess()) {
                return prefix.add(key, result);
            } else {
                result.ifError(error -> logs.add(error.messageSupplier()));
            }
        }
        return prefix.withErrorsFrom(DataResult.error(() -> MultiCodec.buildMsg(logs)));
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, TestedCodec<? extends T>>> iterator() {
        return this.codecs.entrySet().iterator();
    }
}
