package cool.muyucloud.croparia.api.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.reflection.RecordCodecBuilderReflection;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.Ref;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CodecUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Character> read(DynamicOps<T> ops, T input) {
            return ops.getStringValue(input).map(s -> {
                if (s.length() != 1) throw new IllegalArgumentException("Invalid char: " + s);
                return s.charAt(0);
            });
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Character value) {
            return ops.createString(value.toString());
        }
    };

    @ApiStatus.Experimental
    public static <T> MapCodec<T> toMap(Codec<T> codec) {
        return codec instanceof MapCodec.MapCodecCodec<T>(MapCodec<T> map) ? map : MapCodec.assumeMapUnsafe(codec);
    }

    public static <E> MultiCodec<List<E>> listOf(Codec<E> codec) {
        TestedCodec<List<E>> listCodec = TestedCodec.of(Codec.list(codec), list -> list.size() == 1 ? TestedCodec.fail(() -> "Can be applied by singular codec") : TestedCodec.success());
        Codec<List<E>> singularCodec = codec.xmap(Collections::singletonList, List::getFirst);
        return of(listCodec, singularCodec);
    }

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

    /**
     * <p>
     * Create a combined field codec that allows decoding from any of the specified field names, and encoding to the first field name.
     * </p>
     * <p>
     * For example:
     * <pre>{@code
     * MapCodec<MyType> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
     *     CodecUtil.fieldsOf(Codec.INT, "id", "index").forGetter(MyType::getId)
     * ).apply(instance, MyType::new));
     * }</pre>
     * </p>
     * <p>
     * The {@code codec} can decode from either {@code { "id": 123 }} or {@code { "index": 123 }}, and encode into {@code { "id": 123 }}.
     * </p>
     **/
    public static <T> MultiFieldCodec<T> fieldsOf(Codec<T> codec, String... names) {
        List<TestedFieldCodec<? extends T>> list = new ArrayList<>(names.length);
        TestedCodec<T> tested = TestedCodec.of(codec);
        for (String name : names) {
            list.add(tested.fieldOf(name));
        }
        return new MultiFieldCodec<>(list);
    }

    /**
     * <p>
     * Combine multiple field codecs into one.
     * </p>
     * <p>
     * For example:
     * <pre>{@code
     * MapCodec<MyType> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
     *     CodecUtil.fieldsOf(
     *         TestedCodec.of(Codec.INT.xmap(MyId::find, MyId::getIndex), toDecode -> TestedCodec.fail(() -> "Don't like int decoder, use string")).fieldOf("id"),
     *         TestedCodec.of(Codec.STRING).xmap(MyId::forName, MyId::getName).fieldOf("name")
     *     ).forGetter(MyType::getId)
     * ).apply(instance, MyType::new));
     * }</pre>
     * </p>
     * <p>
     * The {@code codec} can decode from either {@code { "id": 123 }} or {@code { "name": "example" }}, and encode into {@code { "name": "example }}.
     * </p>
     *
     * @param codecs the field codecs to combine
     * @param <T>    target type
     * @return the combined field codec
     */
    @SafeVarargs
    public static <T> MultiFieldCodec<T> fieldsOf(TestedFieldCodec<? extends T>... codecs) {
        return new MultiFieldCodec<>(Arrays.asList(codecs));
    }

    /**
     * Map a map codec into stream codec of target type.
     *
     * @param codec  original map codec
     * @param parser transform original type into target type
     * @param getter get original type from target type
     * @return stream codec of target type.
     * @see #mapStream(Codec, Function, Function)
     */
    public static <B extends FriendlyByteBuf, T, I> StreamCodec<B, T> mapStream(MapCodec<I> codec, Function<? super I, ? extends T> parser, Function<? super T, ? extends I> getter) {
        return mapStream(codec.codec(), parser, getter);
    }

    /**
     * Map a codec into stream codec of target type.
     *
     * @param codec  original codec
     * @param parser transform original type into target type
     * @param getter get original type from target type
     * @return stream codec of target type.
     *
     */
    public static <B extends FriendlyByteBuf, T, I> StreamCodec<B, T> mapStream(Codec<I> codec, Function<? super I, ? extends T> parser, Function<? super T, ? extends I> getter) {
        StreamCodec<B, I> stream = toStream(codec);
        return stream.map(parser, getter);
    }

    /**
     * Covert codec into stream codec
     *
     * @param codec the codec
     *
     */
    public static <B extends FriendlyByteBuf, T> StreamCodec<B, T> toStream(Codec<T> codec) {
        return StreamCodec.of((buf, inst) -> buf.writeJsonWithCodec(codec, inst), buf -> buf.readJsonWithCodec(codec));
    }

    public static <B extends FriendlyByteBuf, T> StreamCodec<B, T> toStream(MapCodec<T> codec) {
        return toStream(codec.codec());
    }

    public static <T> JsonElement encodeJson(T object, Codec<T> codec) {
        Ref<JsonElement> elementRef = new Ref<>();
        CropariaIf.getRegistryAccess().ifPresentOrElse(
            access -> elementRef.set(codec.encodeStart(RegistryOps.create(JsonOps.INSTANCE, access), object).getOrThrow()),
            () -> elementRef.set(codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow())
        );
        return elementRef.get();
    }

    public static <T> JsonElement encodeJson(T object, MapCodec<T> codec) {
        return encodeJson(object, codec.codec());
    }

    public static <T> void dumpJson(T object, Codec<T> codec, Path path, boolean override) throws IOException, IllegalStateException {
        JsonElement json = encodeJson(object, codec);
        FileUtil.write(path.toFile(), json.toString(), override);
    }

    public static <T> void dumpJson(T object, MapCodec<T> codec, Path path, boolean override) throws IOException, IllegalStateException {
        dumpJson(object, codec.codec(), path, override);
    }

    public static <T> T decodeJson(JsonElement element, Codec<T> codec) {
        return codec.decode(JsonOps.INSTANCE, element).getOrThrow().getFirst();
    }

    public static <T> T decodeJson(JsonElement element, MapCodec<T> codec) {
        return decodeJson(element, codec.codec());
    }

    public static <T> T readJson(File file, Codec<T> codec) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return decodeJson(GSON.fromJson(reader, JsonElement.class), codec);
        }
    }

    public static <T> T readJson(File file, MapCodec<T> codec) throws IOException {
        return readJson(file, codec.codec());
    }

    /**
     * <p>
     * Extend a base {@link MapCodec} with additional fields to create a new {@link MapCodec}.
     * </p>
     *
     * <p>
     * Example:
     * <pre>{@code
     * MapCodec<Child> childCodec = CodecUtil.extend(
     *     parentCodec,
     *     RecordCodecBuilder.fieldOf("extraField").forGetter(Child::getExtraField),
     *     (parent, extraField) -> new Child(parent.field, extraField)
     * );
     * }</pre>
     * </p>
     *
     */
    @SuppressWarnings("unchecked")
    public static <S, F1, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, BiFunction<S, F1, T> mapper) {
        return extend(superCodec, List.of(field1), (base, fields) -> mapper.apply(base, (F1) fields.getFirst()));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, Function3<S, F1, F2, T> mapper) {
        return extend(superCodec, List.of(field1, field2), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, Function4<S, F1, F2, F3, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, F4, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, RecordCodecBuilder<T, F4> field4, Function5<S, F1, F2, F3, F4, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3, field4), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2), (F4) fields.get(3)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, F4, F5, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, RecordCodecBuilder<T, F4> field4, RecordCodecBuilder<T, F5> field5, Function6<S, F1, F2, F3, F4, F5, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3, field4, field5), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2), (F4) fields.get(3), (F5) fields.get(4)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, F4, F5, F6, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, RecordCodecBuilder<T, F4> field4, RecordCodecBuilder<T, F5> field5, RecordCodecBuilder<T, F6> field6, Function7<S, F1, F2, F3, F4, F5, F6, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3, field4, field5, field6), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2), (F4) fields.get(3), (F5) fields.get(4), (F6) fields.get(5)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, F4, F5, F6, F7, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, RecordCodecBuilder<T, F4> field4, RecordCodecBuilder<T, F5> field5, RecordCodecBuilder<T, F6> field6, RecordCodecBuilder<T, F7> field7, Function8<S, F1, F2, F3, F4, F5, F6, F7, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3, field4, field5, field6, field7), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2), (F4) fields.get(3), (F5) fields.get(4), (F6) fields.get(5), (F7) fields.get(6)));
    }

    @SuppressWarnings("unchecked")
    public static <S, F1, F2, F3, F4, F5, F6, F7, F8, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, RecordCodecBuilder<T, F1> field1, RecordCodecBuilder<T, F2> field2, RecordCodecBuilder<T, F3> field3, RecordCodecBuilder<T, F4> field4, RecordCodecBuilder<T, F5> field5, RecordCodecBuilder<T, F6> field6, RecordCodecBuilder<T, F7> field7, RecordCodecBuilder<T, F8> field8, Function9<S, F1, F2, F3, F4, F5, F6, F7, F8, T> mapper) {
        return extend(superCodec, List.of(field1, field2, field3, field4, field5, field6, field7, field8), (base, fields) -> mapper.apply(base, (F1) fields.getFirst(), (F2) fields.get(1), (F3) fields.get(2), (F4) fields.get(3), (F5) fields.get(4), (F6) fields.get(5), (F7) fields.get(6), (F8) fields.get(7)));
    }

    @SafeVarargs
    public static <S, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, ResultMapper<S, T> resultMapper, RecordCodecBuilder<T, ?>... fieldCodecs) {
        return extend(superCodec, Arrays.asList(fieldCodecs), resultMapper);
    }

    @SuppressWarnings("unchecked")
    private static <S, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, Collection<RecordCodecBuilder<T, ?>> fieldCodecs, ResultMapper<S, T> resultMapper) {
        return MapCodec.of(new MapEncoder.Implementation<>() {
            @Override
            public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
                superCodec.encode(input, ops, prefix);
                for (RecordCodecBuilder<T, ?> fieldCodec : fieldCodecs) {
                    MapEncoder<Object> encoder = (MapEncoder<Object>) RecordCodecBuilderReflection.getEncoder(fieldCodec, input);
                    Function<T, ?> getter = RecordCodecBuilderReflection.getGetter(fieldCodec);
                    Object field = getter.apply(input);
                    encoder.encode(field, ops, prefix);
                }
                return prefix;
            }

            @Override
            public <O> Stream<O> keys(DynamicOps<O> ops) {
                ArrayList<O> keys = new ArrayList<>();
                superCodec.keys(ops).forEach(keys::add);
                for (RecordCodecBuilder<T, ?> fieldCodec : fieldCodecs) {
                    MapDecoder<?> decoder = RecordCodecBuilderReflection.getDecoder(fieldCodec);
                    decoder.keys(ops).forEach(keys::add);
                }
                keys.trimToSize();
                return keys.stream();
            }
        }, new MapDecoder.Implementation<>() {
            @Override
            public <I> DataResult<T> decode(DynamicOps<I> ops, MapLike<I> input) {
                DataResult<S> superResult = superCodec.decode(ops, input);
                if (superResult.isError())
                    return superResult.flatMap(s -> DataResult.error(() -> "Failed to decode super: " + s));
                ArrayList<Object> fields = new ArrayList<>();
                for (RecordCodecBuilder<T, ?> fieldCodec : fieldCodecs) {
                    MapDecoder<?> decoder = RecordCodecBuilderReflection.getDecoder(fieldCodec);
                    DataResult<?> result = decoder.decode(ops, input);
                    if (result.isError())
                        return result.flatMap(f -> DataResult.error(() -> "Failed to decode field: " + f));
                    fields.add(result.getOrThrow());
                }
                return DataResult.success(resultMapper.map(superResult.getOrThrow(), fields));
            }

            @Override
            public <O> Stream<O> keys(DynamicOps<O> ops) {
                ArrayList<O> keys = new ArrayList<>();
                superCodec.keys(ops).forEach(keys::add);
                for (RecordCodecBuilder<T, ?> fieldCodec : fieldCodecs) {
                    MapDecoder<?> decoder = RecordCodecBuilderReflection.getDecoder(fieldCodec);
                    decoder.keys(ops).forEach(keys::add);
                }
                keys.trimToSize();
                return keys.stream();
            }
        });
    }

    public interface ResultMapper<S, T extends S> {
        T map(S superResult, ArrayList<?> partialResults);
    }
}
