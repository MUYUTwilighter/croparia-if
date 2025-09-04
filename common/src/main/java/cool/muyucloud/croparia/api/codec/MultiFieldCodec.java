package cool.muyucloud.croparia.api.codec;

import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MultiFieldCodec<T> extends MapCodec<T> implements Iterable<TestedFieldCodec<? extends T>> {
    private final ArrayList<TestedFieldCodec<? extends T>> codecs = new ArrayList<>();

    public MultiFieldCodec(Collection<TestedFieldCodec<? extends T>> codecs) {
        this.codecs.addAll(codecs);
        this.codecs.trimToSize();
    }

    @Override
    public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
        return this.codecs.stream().map(TestedFieldCodec::getName).map(ops::createString);
    }

    @Override
    public <I> DataResult<T> decode(DynamicOps<I> ops, MapLike<I> input) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.codecs.size());
        for (TestedFieldCodec<? extends T> codec : this.codecs) {
            TestedFieldCodec<T> adapted = codec.adapt();
            DataResult<T> result = adapted.decode(ops, input);
            if (result.isSuccess()) {
                return result;
            } else {
                result.ifError(error -> logs.add(error.messageSupplier()));
            }
        }
        return DataResult.error(() -> {
            StringBuilder builder = new StringBuilder("Failed to decode with any of the provided codecs:");
            for (Supplier<String> log : logs) {
                builder.append("\n- ").append(log.get());
            }
            return builder.toString();
        });
    }

    @Override
    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        ArrayList<String> logs = new ArrayList<>(this.codecs.size());
        for (TestedFieldCodec<? extends T> codec : this.codecs) {
            RecordBuilder<O> result = codec.adapt().encode(input, ops, prefix);
            result.mapError(msg -> {
                logs.add(msg);
                return msg;
            });
        }
        return prefix.withErrorsFrom(DataResult.error(() -> {
            StringBuilder builder = new StringBuilder("Failed to decode with any of the provided codecs:");
            for (String log : logs) {
                builder.append("\n- ").append(log);
            }
            return builder.toString();
        }));
    }

    @Override
    public @NotNull Iterator<TestedFieldCodec<? extends T>> iterator() {
        return this.codecs.iterator();
    }
}
