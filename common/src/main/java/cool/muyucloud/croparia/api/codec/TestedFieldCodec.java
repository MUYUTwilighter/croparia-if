package cool.muyucloud.croparia.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Basically a copy from {@link com.mojang.serialization.codecs.FieldEncoder} and {@link  com.mojang.serialization.codecs.FieldDecoder}, but uses TestedCodec internally.
 **/
public class TestedFieldCodec<T> extends MapCodec<T> {
    private final String name;
    private final TestedCodec<T> codec;

    public TestedFieldCodec(String name, TestedCodec<T> codec) {
        this.codec = codec;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T2> TestedFieldCodec<T2> adapt() {
        return (TestedFieldCodec<T2>) this;
    }

    @Override
    public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.of(ops.createString(name));
    }

    @Override
    public <I> DataResult<T> decode(DynamicOps<I> ops, MapLike<I> input) {
        I value = input.get(name);
        if (value == null) {
            return DataResult.error(() -> "Field '" + name + "' not found");
        }
        return codec.decode(ops, value).map(Pair::getFirst);
    }

    @Override
    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        return prefix.add(name, codec.encodeStart(ops, input));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestedFieldCodec<?> that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(codec, that.codec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, codec);
    }
}
