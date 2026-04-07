package cool.muyucloud.croparia.api.codec;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static cool.muyucloud.croparia.TestSupport.getOrThrow;
import static org.junit.jupiter.api.Assertions.*;

class MultiCodecTest {
    @Test
    void multiCodecUsesFirstSuccessfulCodec() {
        MultiCodec<String> codec = new MultiCodec<>();
        codec.add(CodecUtil.of(
            Codec.STRING,
            value -> TestedCodec.fail(() -> "first-encode-fail"),
            (ops, input) -> TestedCodec.fail(() -> "first-decode-fail")
        ));
        codec.add(CodecUtil.of(Codec.STRING));

        assertEquals("ok", getOrThrow(codec.parse(JsonOps.INSTANCE, new JsonPrimitive("ok"))));
        assertEquals("\"ok\"", getOrThrow(codec.encodeStart(JsonOps.INSTANCE, "ok")).toString());
    }

    @Test
    void multiCodecBuildsErrorMessageWhenAllCodecsFail() {
        MultiCodec<String> codec = new MultiCodec<>();
        codec.add(CodecUtil.of(
            Codec.STRING,
            value -> TestedCodec.fail(() -> "encode-fail-1"),
            (ops, input) -> TestedCodec.fail(() -> "decode-fail-1")
        ));
        codec.add(CodecUtil.of(
            Codec.STRING,
            value -> TestedCodec.fail(() -> "encode-fail-2"),
            (ops, input) -> TestedCodec.fail(() -> "decode-fail-2")
        ));

        var decodeError = codec.parse(JsonOps.INSTANCE, new JsonPrimitive("x")).error().orElseThrow().message();
        var encodeError = codec.encodeStart(JsonOps.INSTANCE, "x").error().orElseThrow().message();

        assertTrue(decodeError.contains("Failed to apply any of the provided codecs"));
        assertTrue(decodeError.contains("decode-fail-1"));
        assertTrue(decodeError.contains("decode-fail-2"));
        assertTrue(encodeError.contains("encode-fail-1"));
        assertTrue(encodeError.contains("encode-fail-2"));
    }

    @Test
    void multiFieldCodecDecodesAliasesAndEncodesFirstSuccessfulField() {
        Map<String, TestedCodec<? extends String>> codecs = new LinkedHashMap<>();
        codecs.put("a", CodecUtil.of(
            Codec.STRING,
            value -> TestedCodec.fail(() -> "a-encode-fail"),
            (ops, input) -> TestedCodec.fail(() -> "a-decode-fail")
        ));
        codecs.put("b", CodecUtil.of(Codec.STRING));
        MultiFieldCodec<String> codec = new MultiFieldCodec<>(codecs);

        JsonObject json = new JsonObject();
        json.addProperty("b", "value");
        String decoded = getOrThrow(codec.codec().parse(JsonOps.INSTANCE, json));
        JsonObject encoded = getOrThrow(codec.codec().encodeStart(JsonOps.INSTANCE, "value")).getAsJsonObject();

        assertEquals("value", decoded);
        assertTrue(encoded.has("b"));
        assertFalse(encoded.has("a"));
    }

    @Test
    void optionalMultiFieldCodecSupportsEmptyAndPresentValues() {
        Map<String, TestedCodec<? extends Integer>> codecs = new LinkedHashMap<>();
        codecs.put("id", CodecUtil.of(Codec.INT));
        OptionalMultiFieldCodec<Integer> codec = new OptionalMultiFieldCodec<>(codecs);

        Optional<Integer> emptyDecoded = getOrThrow(codec.codec().parse(JsonOps.INSTANCE, new JsonObject()));
        JsonObject encodedEmpty = getOrThrow(codec.codec().encodeStart(JsonOps.INSTANCE, Optional.empty())).getAsJsonObject();

        JsonObject withValue = new JsonObject();
        withValue.addProperty("id", 5);
        Optional<Integer> valueDecoded = getOrThrow(codec.codec().parse(JsonOps.INSTANCE, withValue));
        JsonObject encodedValue = getOrThrow(codec.codec().encodeStart(JsonOps.INSTANCE, Optional.of(5))).getAsJsonObject();

        assertTrue(emptyDecoded.isEmpty());
        assertEquals(0, encodedEmpty.size());
        assertEquals(5, valueDecoded.orElseThrow());
        assertTrue(encodedValue.has("id"));
    }

    @Test
    void multiFieldCodecReturnsErrorWhenNoFieldCanDecode() {
        Map<String, TestedCodec<? extends String>> codecs = new LinkedHashMap<>();
        codecs.put("a", CodecUtil.of(Codec.STRING, (ops, input) -> TestedCodec.fail(() -> "a-decode-fail")));
        codecs.put("b", CodecUtil.of(Codec.STRING, (ops, input) -> TestedCodec.fail(() -> "b-decode-fail")));
        MultiFieldCodec<String> codec = new MultiFieldCodec<>(codecs);

        JsonObject json = new JsonObject();
        json.addProperty("a", "x");
        var error = codec.codec().parse(JsonOps.INSTANCE, json).error().orElseThrow().message();
        assertTrue(error.contains("a-decode-fail"));
        assertTrue(error.contains("b-decode-fail"));
    }

    @Test
    void optionalMultiFieldCodecReturnsErrorForPresentButInvalidValue() {
        Map<String, TestedCodec<? extends Integer>> codecs = new LinkedHashMap<>();
        codecs.put("id", CodecUtil.of(Codec.INT, (ops, input) -> TestedCodec.fail(() -> "id-invalid")));
        OptionalMultiFieldCodec<Integer> codec = new OptionalMultiFieldCodec<>(codecs);

        JsonObject json = new JsonObject();
        json.addProperty("id", 3);
        var error = codec.codec().parse(JsonOps.INSTANCE, json).error().orElseThrow().message();
        assertTrue(error.contains("id-invalid"));
    }

    @Test
    void optionalMultiFieldCodecReturnsEncodeErrorWhenAllFieldsRejectValue() {
        Map<String, TestedCodec<? extends Integer>> codecs = new LinkedHashMap<>();
        codecs.put("id", CodecUtil.of(Codec.INT, value -> TestedCodec.fail(() -> "id-encode-fail")));
        OptionalMultiFieldCodec<Integer> codec = new OptionalMultiFieldCodec<>(codecs);

        var error = codec.codec().encodeStart(JsonOps.INSTANCE, Optional.of(8)).error().orElseThrow().message();
        assertTrue(error.contains("id-encode-fail"));
    }

    @Test
    void multiFieldAndOptionalIteratorsExposeConfiguredKeys() {
        Map<String, TestedCodec<? extends Integer>> codecs = new LinkedHashMap<>();
        codecs.put("x", CodecUtil.of(Codec.INT));
        codecs.put("y", CodecUtil.of(Codec.INT));

        MultiFieldCodec<Integer> multi = new MultiFieldCodec<>(codecs);
        OptionalMultiFieldCodec<Integer> optional = new OptionalMultiFieldCodec<>(codecs);

        var multiKeys = StreamSupport.stream(multi.spliterator(), false).map(Map.Entry::getKey).toList();
        var optionalKeys = StreamSupport.stream(optional.spliterator(), false).map(Map.Entry::getKey).toList();
        assertEquals(java.util.List.of("x", "y"), multiKeys);
        assertEquals(java.util.List.of("x", "y"), optionalKeys);
    }
}
