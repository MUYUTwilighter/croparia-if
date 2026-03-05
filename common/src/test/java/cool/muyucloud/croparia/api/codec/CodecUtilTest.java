package cool.muyucloud.croparia.api.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodecUtilTest {
    @Test
    void listOfDecodesFromSingleAndListAndEncodesCompactly() {
        MultiCodec<List<Integer>> codec = CodecUtil.listOf(Codec.INT);
        JsonArray jsonList = new JsonArray();
        jsonList.add(1);
        jsonList.add(2);

        List<Integer> fromSingle = codec.parse(JsonOps.INSTANCE, new JsonPrimitive(5)).getOrThrow();
        List<Integer> fromList = codec.parse(JsonOps.INSTANCE, jsonList).getOrThrow();

        assertEquals(List.of(5), fromSingle);
        assertEquals(List.of(1, 2), fromList);
        assertEquals("7", codec.encodeStart(JsonOps.INSTANCE, List.of(7)).getOrThrow().toString());
        assertEquals("[7,8]", codec.encodeStart(JsonOps.INSTANCE, List.of(7, 8)).getOrThrow().toString());
    }

    @Test
    void fieldsOfSupportsAliasDecodeAndPrimaryEncode() {
        var mapCodec = CodecUtil.fieldsOf(Codec.INT, "id", "index").codec();
        JsonObject byAlias = new JsonObject();
        byAlias.addProperty("index", 12);

        int decoded = mapCodec.parse(JsonOps.INSTANCE, byAlias).getOrThrow();
        JsonObject encoded = mapCodec.encodeStart(JsonOps.INSTANCE, 12).getOrThrow().getAsJsonObject();

        assertEquals(12, decoded);
        assertTrue(encoded.has("id"));
        assertFalse(encoded.has("index"));
    }

    @Test
    void optionalFieldsOfReturnsDefaultWhenMissing() {
        var mapCodec = CodecUtil.optionalFieldsOf(Codec.INT, 99, "id", "index").codec();
        JsonObject empty = new JsonObject();

        int decoded = mapCodec.parse(JsonOps.INSTANCE, empty).getOrThrow();
        JsonObject encodedDefault = mapCodec.encodeStart(JsonOps.INSTANCE, 99).getOrThrow().getAsJsonObject();
        JsonObject encodedCustom = mapCodec.encodeStart(JsonOps.INSTANCE, 5).getOrThrow().getAsJsonObject();

        assertEquals(99, decoded);
        assertEquals(0, encodedDefault.size());
        assertTrue(encodedCustom.has("id"));
    }

    @Test
    void testedCodecEncodeAndDecodeTestsGateOperations() {
        TestedCodec<Integer> codec = CodecUtil.of(
            Codec.INT,
            value -> value >= 0 ? TestedCodec.success() : TestedCodec.fail(() -> "neg"),
            (ops, input) -> {
                DataResult<Number> number = ops.getNumberValue(input);
                return number.isSuccess() && number.getOrThrow().intValue() >= 0
                    ? TestedCodec.success()
                    : TestedCodec.fail(() -> "bad");
            }
        );

        assertTrue(codec.encodeStart(JsonOps.INSTANCE, 3).isSuccess());
        assertTrue(codec.parse(JsonOps.INSTANCE, new JsonPrimitive(3)).isSuccess());
        assertTrue(codec.encodeStart(JsonOps.INSTANCE, -1).isError());
        assertTrue(codec.parse(JsonOps.INSTANCE, new JsonPrimitive(-1)).isError());
    }

    @Test
    void toMapWrapsNonMapCodecAndKeepsMapCodec() {
        assertNotNull(CodecUtil.toMap(Codec.INT));

        var original = Codec.INT.fieldOf("value");
        assertSame(original, CodecUtil.toMap(original.codec()));
    }

    @Test
    void charCodecRoundTripAndValidation() {
        assertEquals("a", CodecUtil.CHAR.encodeStart(JsonOps.INSTANCE, 'a').getOrThrow().getAsString());
        assertEquals('b', CodecUtil.CHAR.parse(JsonOps.INSTANCE, new JsonPrimitive("b")).getOrThrow());
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.CHAR.parse(JsonOps.INSTANCE, new JsonPrimitive("xx")));
    }

    @Test
    void readJsonStringWorksWithoutGameRegistryContext() {
        assertEquals(5, CodecUtil.readJson("5", Codec.INT).getOrThrow());
        assertThrows(RuntimeException.class, () -> CodecUtil.readJson("{", Codec.INT));
    }
}
