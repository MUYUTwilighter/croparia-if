package cool.muyucloud.croparia.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * A codec that tries multiple codecs in order for encoding and decoding.
 * Returns the first successful result, or an error if all fail.
 *
 * @param <T> the type to encode/decode
 * @see TestedCodec
 */
public class MultiCodec<T> extends ArrayList<TestedCodec<? extends T>> implements Codec<T> {
    @SafeVarargs
    public static <T> MultiCodec<T> of(Codec<? extends T>... codecs) {
        return of(toEncode -> TestedCodec.success(), (ops, toDecode) -> TestedCodec.success(), codecs);
    }

    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.EncodeTest<T> encodeTest, Codec<? extends T>... codecs) {
        return of(encodeTest, (ops, toDecode) -> TestedCodec.success(), codecs);
    }

    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.DecodeTest<T> decodeTest, Codec<? extends T>... codecs) {
        return of(toEncode -> TestedCodec.success(), decodeTest, codecs);
    }

    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.EncodeTest<T> encodeTest, TestedCodec.DecodeTest<?> decodeTest, Codec<? extends T>... codecs) {
        MultiCodec<T> result = new MultiCodec<>();
        for (Codec<? extends T> codec : codecs) {
            if (codec instanceof TestedCodec<? extends T> testedCodec) {
                result.add(testedCodec);
            } else {
                result.add(TestedCodec.of(codec, encodeTest.adapt(), decodeTest));
            }
        }
        return result;
    }

    @Override
    public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I toDecode) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.size());
        for (TestedCodec<? extends T> codec : this) {
            TestedCodec<T> adapted = codec.adapt();
            DataResult<Pair<T, I>> result = adapted.decode(ops, toDecode);
            if (result.isSuccess()) {
                return result;
            } else {
                result.error().ifPresent(error -> logs.add(error.messageSupplier()));
            }
        }
        return DataResult.error(() -> buildMsg(logs));
    }

    @Override
    public <O> DataResult<O> encode(T toEncode, DynamicOps<O> ops, O prefix) {
        ArrayList<Supplier<String>> logs = new ArrayList<>(this.size());
        for (TestedCodec<? extends T> codec : this) {
            DataResult<O> result = codec.adapt().encode(toEncode, ops, prefix);
            if (result.isSuccess()) {
                return result;
            } else {
                result.error().ifPresent(error -> logs.add(error.messageSupplier()));
            }
        }
        return DataResult.error(() -> buildMsg(logs));
    }

    private static String buildMsg(Iterable<Supplier<String>> logs) {
        StringBuilder builder = new StringBuilder("Failed to apply any of the provided codecs:");
        for (Supplier<String> log : logs) {
            builder.append("\n").append(log.get());
        }
        return builder.toString();
    }
}
