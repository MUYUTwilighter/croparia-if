package cool.muyucloud.croparia.reflection;

import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.LazySupplier;

import java.lang.reflect.Field;
import java.util.function.Function;

public class RecordCodecBuilderReflection {
    public static final LazySupplier<Field> GETTER = CifUtil.forField(RecordCodecBuilder.class, "getter");
    public static final LazySupplier<Field> ENCODER = CifUtil.forField(RecordCodecBuilder.class, "encoder");
    public static final LazySupplier<Field> DECODER = CifUtil.forField(RecordCodecBuilder.class, "decoder");

    @SuppressWarnings("unchecked")
    public static <T, F> MapEncoder<F> getEncoder(RecordCodecBuilder<T, F> builder, T instance) {
        try {
            return ((Function<T, MapEncoder<F>>) ENCODER.get().get(builder)).apply(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, F> Function<T, F> getGetter(RecordCodecBuilder<T, F> builder) {
        try {
            return (Function<T, F>) GETTER.get().get(builder);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, F> MapDecoder<F> getDecoder(RecordCodecBuilder<T, F> builder) {
        try {
            return (MapDecoder<F>) DECODER.get().get(builder);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
