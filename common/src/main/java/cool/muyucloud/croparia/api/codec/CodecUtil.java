package cool.muyucloud.croparia.api.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.json.JsonTransformer;
import cool.muyucloud.croparia.reflection.RecordCodecBuilderReflection;
import cool.muyucloud.croparia.util.FileUtil;
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
import java.util.function.Supplier;
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

    /**
     * Convert a codec into map codec if possible.<br/>
     * If the input codec is already a map codec, it will be returned as is.<br/>
     * Otherwise, it will be unsafely converted into a map codec. For example, {@code "example"} will be converted
     * into {@code { "value": "example" }}.
     *
     * @param codec the codec to convert
     * @param <T>   the type of the codec
     * @return the map codec
     * @see MapCodec#assumeMapUnsafe(Codec)
     */
    @ApiStatus.Experimental
    public static <T> MapCodec<T> toMap(Codec<T> codec) {
        return codec instanceof MapCodec.MapCodecCodec<T>(MapCodec<T> map) ? map : MapCodec.assumeMapUnsafe(codec);
    }

    /**
     * Create a flexible list codec that can decode from either a single element or a list of elements into a list of elements,
     * and encode into a single element if the list has exactly one element, or a list otherwise.
     *
     * @param codec the codec for the element type
     * @param <E>   the element type
     * @return the flexible list codec
     * @see MultiCodec
     */
    public static <E> MultiCodec<List<E>> listOf(Codec<E> codec) {
        TestedCodec<List<E>> listCodec = of(Codec.list(codec), list -> list.size() == 1 ? TestedCodec.fail(() -> "Can be applied by singular codec") : TestedCodec.success());
        Codec<List<E>> singularCodec = codec.xmap(Collections::singletonList, List::getFirst);
        return of(listCodec, singularCodec);
    }

    /**
     * Create a MultiCodec that tries multiple codecs in order for encoding and decoding.<br/>
     * Returns the first successful result, or an error if all fail.<br/>
     * You may try {@link #of(Codec, TestedCodec.EncodeTest, TestedCodec.DecodeTest)} to test before encoding or decoding.
     *
     * @param codecs candidate codecs
     * @param <T>    target type
     * @return the combined codec
     *
     */
    @SafeVarargs
    public static <T> MultiCodec<T> of(Codec<? extends T>... codecs) {
        return of(toEncode -> TestedCodec.success(), (ops, toDecode) -> TestedCodec.success(), codecs);
    }

    /**
     * Create a MultiCodec that tries multiple codecs in order for encoding and decoding.<br/>
     * Before encoding, it will apply the specified encode test to the input object.<br/>
     * It directly tries decoding with each codec without any test.<br/>
     *
     * @param encodeTest Test for encode
     * @param codecs     candidate codecs
     * @param <T>        target type
     * @return the combined codec
     * @see TestedCodec.EncodeTest
     * @see TestedCodec#success()
     * @see TestedCodec#fail()
     * @see TestedCodec#fail(Supplier)
     **/
    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.EncodeTest<T> encodeTest, Codec<? extends T>... codecs) {
        return of(encodeTest, (ops, toDecode) -> TestedCodec.success(), codecs);
    }

    /**
     * Create a MultiCodec that tries multiple codecs in order for encoding and decoding.<br/>
     * Before decoding, it will apply the specified decode test to the input data.<br/>
     * It directly tries encoding with each codec without any test.<br/>
     *
     * @param decodeTest Test for decode
     * @param codecs     candidate codecs
     * @param <T>        target type
     * @return the combined codec
     * @see TestedCodec.DecodeTest
     * @see TestedCodec#success()
     * @see TestedCodec#fail()
     * @see TestedCodec#fail(Supplier)
     */
    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.DecodeTest<T> decodeTest, Codec<? extends T>... codecs) {
        return of(toEncode -> TestedCodec.success(), decodeTest, codecs);
    }

    /**
     * Create a MultiCodec that tries multiple codecs in order for encoding and decoding.<br/>
     * Before encoding, it will apply the specified encode test to the input object.<br/>
     * Before decoding, it will apply the specified decode test to the input data.<br/>
     *
     * @param encodeTest Test for encode
     * @param decodeTest Test for decode
     * @param codecs     candidate codecs
     * @param <T>        target type
     * @return the combined codec
     * @see TestedCodec.EncodeTest
     * @see TestedCodec.DecodeTest
     * @see TestedCodec#success()
     * @see TestedCodec#fail()
     * @see TestedCodec#fail(Supplier)
     */
    @SafeVarargs
    public static <T> MultiCodec<T> of(TestedCodec.EncodeTest<T> encodeTest, TestedCodec.DecodeTest<?> decodeTest, Codec<? extends T>... codecs) {
        MultiCodec<T> result = new MultiCodec<>();
        for (Codec<? extends T> codec : codecs) {
            if (codec instanceof TestedCodec<? extends T> testedCodec) {
                result.add(testedCodec);
            } else {
                result.add(of(codec, encodeTest.adapt(), decodeTest));
            }
        }
        return result;
    }

    /**
     * Create a TestedCodec that only wraps the input codec without any tests.
     */
    public static <T> TestedCodec<T> of(Codec<T> codec) {
        return new TestedCodec<>(codec, o -> TestedCodec.success(), (ops, input) -> TestedCodec.success());
    }

    /**
     * Create a TestedCodec with specified encode test.<br/>
     * It will directly try decoding with input codec without any test, and apply the encode test before encoding.
     *
     * @see #of(Codec, TestedCodec.EncodeTest, TestedCodec.DecodeTest)
     */
    public static <T> TestedCodec<T> of(Codec<T> codec, TestedCodec.EncodeTest<T> encodeTest) {
        return new TestedCodec<>(codec, encodeTest, (ops, input) -> TestedCodec.success());
    }

    /**
     * Creates a TestedCodec with a specified decode test.<br/>
     * It will directly try encoding with input codec without any test, and apply the decode test before decoding.
     *
     * @see #of(Codec, TestedCodec.EncodeTest, TestedCodec.DecodeTest)
     */
    public static <T> TestedCodec<T> of(Codec<T> codec, TestedCodec.DecodeTest<?> decodeTest) {
        return new TestedCodec<>(codec, o -> TestedCodec.success(), decodeTest);
    }

    /**
     * Creates a TestedCodec with specified encode and decode tests.
     *
     * @param codec      The Codec to wrap
     * @param encodeTest The test to apply before encoding
     * @param decodeTest The test to apply before decoding
     * @param <T>        The type of the target object
     * @return A TestedCodec with the specified encode and decode tests
     * @see TestedCodec.EncodeTest
     * @see TestedCodec.DecodeTest
     * @see TestedCodec#success()
     * @see TestedCodec#fail()
     * @see TestedCodec#fail(Supplier)
     */
    public static <T> TestedCodec<T> of(Codec<T> codec, TestedCodec.EncodeTest<T> encodeTest, TestedCodec.DecodeTest<?> decodeTest) {
        return new TestedCodec<>(codec, encodeTest, decodeTest);
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
        Map<String, TestedCodec<? extends T>> map = new LinkedHashMap<>();
        TestedCodec<T> tested = of(codec);
        for (String name : names) {
            map.put(name, tested);
        }
        return new MultiFieldCodec<>(map);
    }

    public static <T> MultiFieldCodec<T> fieldsOf(Map<String, TestedCodec<? extends T>> map) {
        return new MultiFieldCodec<>(map);
    }

    public static <T> OptionalMultiFieldCodec<T> optionalFieldsOf(Codec<T> codec, String... names) {
        Map<String, TestedCodec<? extends T>> map = new LinkedHashMap<>();
        TestedCodec<T> tested = of(codec);
        for (String name : names) {
            map.put(name, tested);
        }
        return new OptionalMultiFieldCodec<>(map);
    }

    public static <T> OptionalMultiFieldCodec<T> optionalFieldsOf(Map<String, TestedCodec<? extends T>> map) {
        return new OptionalMultiFieldCodec<>(map);
    }

    public static <T> MapCodec<T> optionalFieldsOf(Codec<T> codec, T def, String... names) {
        Map<String, TestedCodec<? extends T>> map = new LinkedHashMap<>();
        TestedCodec<T> tested = of(codec);
        for (String name : names) {
            map.put(name, tested);
        }
        return optionalFieldsOf(map, def);
    }

    public static <T> MapCodec<T> optionalFieldsOf(Map<String, TestedCodec<? extends T>> map, T def) {
        return new OptionalMultiFieldCodec<>(map).xmap(
            may -> may.orElse(def),
            t -> Objects.equals(t, def) ? Optional.empty() : Optional.of(t)
        );
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

    public static <T> Optional<RegistryOps<T>> getRegistryOps(DynamicOps<T> ops) {
        return CropariaIf.getRegistryAccess().map(access -> RegistryOps.create(ops, access));
    }

    public static <T> DynamicOps<T> getOps(DynamicOps<T> ops) {
        return getRegistryOps(ops).map(o -> (DynamicOps<T>) o).orElse(ops);
    }

    public static <T> DataResult<JsonElement> encodeJson(T object, Codec<T> codec) {
        return codec.encodeStart(getOps(JsonOps.INSTANCE), object);
    }

    public static <T> DataResult<JsonElement> encodeJson(T object, MapCodec<T> codec) {
        return encodeJson(object, codec.codec());
    }

    public static <T> DataResult<JsonElement> dumpJson(T object, Codec<T> codec, Path path, boolean override) throws IOException {
        DataResult<JsonElement> result = encodeJson(object, codec);
        if (result.isSuccess()) {
            FileUtil.write(path.toFile(), result.getOrThrow().toString(), override);
        }
        return result;
    }

    public static <T> DataResult<JsonElement> dumpJson(T object, MapCodec<T> codec, Path path, boolean override) throws IOException, IllegalStateException {
        return dumpJson(object, codec.codec(), path, override);
    }

    public static <T> DataResult<T> decodeJson(JsonElement element, Codec<T> codec) {
        return codec.decode(getOps(JsonOps.INSTANCE), element).map(Pair::getFirst);
    }

    public static <T> DataResult<T> decodeJson(JsonElement element, MapCodec<T> codec) {
        return decodeJson(element, codec.codec());
    }

    public static <T> DataResult<T> readJson(File file, Codec<T> codec) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            String filename = file.getName();
            int i = filename.lastIndexOf('.');
            if (i < 0 || i + 1 == filename.length()) throw new IOException("No file extension found in " + filename);
            String subfix = filename.substring(i + 1).toLowerCase();
            JsonTransformer transformer = JsonTransformer.TRANSFORMERS.get(subfix);
            if (transformer == null) throw new IOException("No transformer found for file extension: " + subfix);
            JsonElement json = JsonTransformer.transform(file);
            return decodeJson(json, codec);
        }
    }

    public static <T> DataResult<T> readJson(String json, Codec<T> codec) {
        return decodeJson(GSON.fromJson(json, JsonElement.class), codec);
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
     * @param superCodec The base {@link MapCodec} to extend.
     *                   Must produce a type that is a supertype of the target type {@code T}.
     * @param field1     The first additional field codec.
     * @param mapper     A function that takes the base type and the additional field(s) to produce an instance of the target type {@code T}.
     * @param <S>        The base type produced by the {@code superCodec}.
     * @param <T>        The extended type.
     * @param <F1>       The type of the first additional field.
     * @return A new {@link MapCodec} that combines the base codec and the additional field(s).
     * @apiNote The overloaded methods support up to 8 additional fields, see below.
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

    /**
     * A raw version of {@link #extend(MapCodec, Collection, ResultMapper)} that accepts a collection of field codecs.<br/>
     * This method is useful when you have a dynamic number of field codecs to combine.<br/>
     * However, you need to manually handle the type-casting of the additional fields in the {@link ResultMapper}.
     *
     * @param superCodec   The base {@link MapCodec} to extend.
     * @param resultMapper A function that takes the base type and a list of additional fields to produce an instance of the target type {@code T}.
     * @param fieldCodecs  A collection of additional field codecs.
     * @param <S>          The base type produced by the {@code superCodec}.
     * @param <T>          The extended type.
     * @return A new {@link MapCodec} that combines the base codec and the additional field(s).
     * @apiNote This method is less type-safe than the overloaded methods, as it relies on manual casting in the {@link ResultMapper}.
     * Use with caution and ensure that the order and types of the field codecs match the expectations in the {@link ResultMapper}.
     *
     */
    @SafeVarargs
    public static <S, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, ResultMapper<S, T> resultMapper, RecordCodecBuilder<T, ?>... fieldCodecs) {
        return extend(superCodec, Arrays.asList(fieldCodecs), resultMapper);
    }

    /**
     * @see #extend(MapCodec, ResultMapper, RecordCodecBuilder[])
     *
     */
    @SuppressWarnings("unchecked")
    public static <S, T extends S> MapCodec<T> extend(MapCodec<S> superCodec, Collection<RecordCodecBuilder<T, ?>> fieldCodecs, ResultMapper<S, T> resultMapper) {
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
                return DataResult.success(resultMapper.apply(superResult.getOrThrow(), fields));
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

    /**
     * A function that takes a base type and a list of additional fields to produce an instance of the target type {@code T}.
     * Used in {@link #extend(MapCodec, Collection, ResultMapper)} to combine a base codec with additional field codecs.
     *
     * @param <S> The base type produced by the super codec.
     * @param <T> The extended type.
     */
    public interface ResultMapper<S, T extends S> extends BiFunction<S, ArrayList<?>, T> {
        @Override
        T apply(S superResult, ArrayList<?> partialResults);
    }
}
