package cool.muyucloud.croparia.api.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cool.muyucloud.croparia.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class CodecUtilTest {
    @Test
    void listOfDecodesFromSingleAndListAndEncodesCompactly() {
        MultiCodec<List<Integer>> codec = CodecUtil.listOf(Codec.INT);
        JsonArray jsonList = new JsonArray();
        jsonList.add(1);
        jsonList.add(2);

        List<Integer> fromSingle = getOrThrow(codec.parse(JsonOps.INSTANCE, new JsonPrimitive(5)));
        List<Integer> fromList = getOrThrow(codec.parse(JsonOps.INSTANCE, jsonList));

        assertEquals(List.of(5), fromSingle);
        assertEquals(List.of(1, 2), fromList);
        assertEquals("7", getOrThrow(codec.encodeStart(JsonOps.INSTANCE, List.of(7))).toString());
        assertEquals("[7,8]", getOrThrow(codec.encodeStart(JsonOps.INSTANCE, List.of(7, 8))).toString());
    }

    @Test
    void fieldsOfSupportsAliasDecodeAndPrimaryEncode() {
        var mapCodec = CodecUtil.fieldsOf(Codec.INT, "id", "index").codec();
        JsonObject byAlias = new JsonObject();
        byAlias.addProperty("index", 12);

        int decoded = getOrThrow(mapCodec.parse(JsonOps.INSTANCE, byAlias));
        JsonObject encoded = getOrThrow(mapCodec.encodeStart(JsonOps.INSTANCE, 12)).getAsJsonObject();

        assertEquals(12, decoded);
        assertTrue(encoded.has("id"));
        assertFalse(encoded.has("index"));
    }

    @Test
    void optionalFieldsOfReturnsDefaultWhenMissing() {
        var mapCodec = CodecUtil.optionalFieldsOf(Codec.INT, 99, "id", "index").codec();
        JsonObject empty = new JsonObject();

        int decoded = getOrThrow(mapCodec.parse(JsonOps.INSTANCE, empty));
        JsonObject encodedDefault = getOrThrow(mapCodec.encodeStart(JsonOps.INSTANCE, 99)).getAsJsonObject();
        JsonObject encodedCustom = getOrThrow(mapCodec.encodeStart(JsonOps.INSTANCE, 5)).getAsJsonObject();

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
                return isSuccess(number) && getOrThrow(number).intValue() >= 0
                    ? TestedCodec.success()
                    : TestedCodec.fail(() -> "bad");
            }
        );

        assertTrue(isSuccess(codec.encodeStart(JsonOps.INSTANCE, 3)));
        assertTrue(isSuccess(codec.parse(JsonOps.INSTANCE, new JsonPrimitive(3))));
        assertTrue(isError(codec.encodeStart(JsonOps.INSTANCE, -1)));
        assertTrue(isError(codec.parse(JsonOps.INSTANCE, new JsonPrimitive(-1))));
    }

    @Test
    void ofOverloadsWithSingleTestAreApplied() {
        MultiCodec<Integer> encodeOnly = CodecUtil.of(value -> value > 0 ? TestedCodec.success() : TestedCodec.fail(), Codec.INT);
        MultiCodec<Integer> decodeOnly = CodecUtil.of((ops, input) -> {
            DataResult<Number> num = ops.getNumberValue(input);
            return isSuccess(num) && getOrThrow(num).intValue() > 0 ? TestedCodec.success() : TestedCodec.fail();
        }, Codec.INT);

        assertTrue(isSuccess(encodeOnly.encodeStart(JsonOps.INSTANCE, 2)));
        assertTrue(isError(encodeOnly.encodeStart(JsonOps.INSTANCE, 0)));
        assertNotNull(decodeOnly);
        assertTrue(isSuccess(decodeOnly.encodeStart(JsonOps.INSTANCE, 2)));
    }

    @Test
    void fieldsAndOptionalFieldsOfMapOverloadsWork() {
        Map<String, TestedCodec<? extends Integer>> aliases = new LinkedHashMap<>();
        aliases.put("id", CodecUtil.of(Codec.INT));
        aliases.put("index", CodecUtil.of(Codec.INT));

        var fieldsCodec = CodecUtil.fieldsOf(aliases).codec();
        JsonObject byAlias = new JsonObject();
        byAlias.addProperty("index", 23);
        assertEquals(23, getOrThrow(fieldsCodec.parse(JsonOps.INSTANCE, byAlias)));

        var optionalCodec = CodecUtil.optionalFieldsOf(Codec.INT, "id", "index").codec();
        assertTrue(getOrThrow(optionalCodec.parse(JsonOps.INSTANCE, new JsonObject())).isEmpty());

        var optionalMapCodec = CodecUtil.optionalFieldsOf(aliases).codec();
        assertEquals(23, getOrThrow(optionalMapCodec.parse(JsonOps.INSTANCE, byAlias)).orElseThrow());
    }

    @Test
    void dumpAndReadJsonFileHandleSuccessAndErrorBranches(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("value.json");
        DataResult<?> dumped = CodecUtil.dumpJson(6, Codec.INT, output, true);
        assertTrue(isSuccess(dumped));
        assertEquals("6", Files.readString(output));

        assertEquals(6, getOrThrow(CodecUtil.readJson(output.toFile(), Codec.INT)));

        Path noExt = tempDir.resolve("value");
        Files.writeString(noExt, "6");
        assertThrows(IOException.class, () -> CodecUtil.readJson(noExt.toFile(), Codec.INT));

        Path unknown = tempDir.resolve("value.unknown");
        Files.writeString(unknown, "6");
        assertThrows(IOException.class, () -> CodecUtil.readJson(unknown.toFile(), Codec.INT));
    }

    @Test
    void toMapWrapsNonMapCodecAndKeepsMapCodec() {
        assertNotNull(CodecUtil.toMap(Codec.INT));

        var original = Codec.INT.fieldOf("value");
        assertSame(original, CodecUtil.toMap(original.codec()));
    }

    @Test
    void charCodecRoundTripAndValidation() {
        assertEquals("a", getOrThrow(CodecUtil.CHAR.encodeStart(JsonOps.INSTANCE, 'a')).getAsString());
        assertEquals('b', getOrThrow(CodecUtil.CHAR.parse(JsonOps.INSTANCE, new JsonPrimitive("b"))));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.CHAR.parse(JsonOps.INSTANCE, new JsonPrimitive("xx")));
    }

    @Test
    void readJsonStringWorksWithoutGameRegistryContext() {
        assertEquals(5, getOrThrow(CodecUtil.readJson("5", Codec.INT)));
        assertThrows(RuntimeException.class, () -> CodecUtil.readJson("{", Codec.INT));
    }
}
