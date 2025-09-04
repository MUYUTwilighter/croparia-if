package cool.muyucloud.croparia.reflection;

import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.lang.reflect.Field;
import java.util.function.Function;

public class RecordCodecBuilderReflection {
    public static final Field GETTER;
    public static final Field ENCODER;
    public static final Field DECODER;

    static {
        try {
            GETTER = RecordCodecBuilder.class.getDeclaredField("getter");
            GETTER.setAccessible(true);
            ENCODER = RecordCodecBuilder.class.getDeclaredField("encoder");
            ENCODER.setAccessible(true);
            DECODER = RecordCodecBuilder.class.getDeclaredField("decoder");
            DECODER.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, F> MapEncoder<F> getEncoder(RecordCodecBuilder<T, F> builder, T instance) {
        try {
            return ((Function<T, MapEncoder<F>>) ENCODER.get(builder)).apply(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, F> Function<T, F> getGetter(RecordCodecBuilder<T, F> builder) {
        try {
            return (Function<T, F>) GETTER.get(builder);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, F> MapDecoder<F> getDecoder(RecordCodecBuilder<T, F> builder) {
        try {
            return (MapDecoder<F>) DECODER.get(builder);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
