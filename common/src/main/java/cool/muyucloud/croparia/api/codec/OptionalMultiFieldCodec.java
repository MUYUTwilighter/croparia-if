package cool.muyucloud.croparia.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class OptionalMultiFieldCodec<T> extends MapCodec<Optional<T>> implements Iterable<Map.Entry<String, TestedCodec<? extends T>>> {
    private final Map<String, TestedCodec<? extends T>> codecs = new LinkedHashMap<>();

    public OptionalMultiFieldCodec(Map<String, TestedCodec<? extends T>> codecs) {
        this.codecs.putAll(codecs);
    }

    @Override
    public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
        return this.codecs.keySet().stream().map(ops::createString);
    }

    @Override
    public <I> DataResult<Optional<T>> decode(DynamicOps<I> ops, MapLike<I> input) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.codecs.size());
        int skipped = 0;
        for (var entry : this.codecs.entrySet()) {
            String key = entry.getKey();
            if (input.get(key) == null) {
                skipped++;
                continue;
            }
            TestedCodec<T> adapted = entry.getValue().adapt();
            DataResult<Pair<T, I>> result = adapted.decode(ops, input.get(key));
            if (result.isSuccess()) {
                return result.map(pair -> Optional.ofNullable(pair.getFirst()));
            } else {
                result.ifError(error -> logs.add(error.messageSupplier()));
            }
        }
        return skipped == this.codecs.size() ? DataResult.success(Optional.empty()) : DataResult.error(() -> MultiCodec.buildMsg(logs));
    }

    @Override
    public <O> RecordBuilder<O> encode(Optional<T> input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        if (input.isEmpty()) return prefix;
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.codecs.size());
        for (var entry : this.codecs.entrySet()) {
            String key = entry.getKey();
            TestedCodec<T> adapted = entry.getValue().adapt();
            DataResult<O> result = adapted.encodeStart(ops, input.get());
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
