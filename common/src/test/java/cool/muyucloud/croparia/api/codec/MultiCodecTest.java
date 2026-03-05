package cool.muyucloud.croparia.api.codec;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

        assertEquals("ok", codec.parse(JsonOps.INSTANCE, new JsonPrimitive("ok")).getOrThrow());
        assertEquals("\"ok\"", codec.encodeStart(JsonOps.INSTANCE, "ok").getOrThrow().toString());
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
        String decoded = codec.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        JsonObject encoded = codec.codec().encodeStart(JsonOps.INSTANCE, "value").getOrThrow().getAsJsonObject();

        assertEquals("value", decoded);
        assertTrue(encoded.has("b"));
        assertFalse(encoded.has("a"));
    }

    @Test
    void optionalMultiFieldCodecSupportsEmptyAndPresentValues() {
        Map<String, TestedCodec<? extends Integer>> codecs = new LinkedHashMap<>();
        codecs.put("id", CodecUtil.of(Codec.INT));
        OptionalMultiFieldCodec<Integer> codec = new OptionalMultiFieldCodec<>(codecs);

        Optional<Integer> emptyDecoded = codec.codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        JsonObject encodedEmpty = codec.codec().encodeStart(JsonOps.INSTANCE, Optional.empty()).getOrThrow().getAsJsonObject();

        JsonObject withValue = new JsonObject();
        withValue.addProperty("id", 5);
        Optional<Integer> valueDecoded = codec.codec().parse(JsonOps.INSTANCE, withValue).getOrThrow();
        JsonObject encodedValue = codec.codec().encodeStart(JsonOps.INSTANCE, Optional.of(5)).getOrThrow().getAsJsonObject();

        assertTrue(emptyDecoded.isEmpty());
        assertEquals(0, encodedEmpty.size());
        assertEquals(5, valueDecoded.orElseThrow());
        assertTrue(encodedValue.has("id"));
    }
}
