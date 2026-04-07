package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import org.junit.jupiter.api.Test;

import static cool.muyucloud.croparia.TestSupport.getOrThrow;
import static cool.muyucloud.croparia.TestSupport.isError;
import static org.junit.jupiter.api.Assertions.*;

class JsonCodecBuilderTest {
    @Test
    void listBuilderAddWithCodecAddsEncodedValues() {
        ListBuilder builder = new ListBuilder().add(Codec.INT, 1, 2, 3);
        JsonArray json = builder.build();
        assertEquals(3, json.size());
        assertEquals(1, json.get(0).getAsInt());
        assertEquals(3, json.get(2).getAsInt());
    }

    @Test
    void listBuilderAddWithCodecThrowsWhenEncodingFails() {
        Codec<Integer> failCodec = CodecUtil.of(
            Codec.INT,
            value -> value < 0 ? TestedCodec.fail(() -> "negative") : TestedCodec.success()
        );
        assertThrows(RuntimeException.class, () -> new ListBuilder().add(failCodec, -1));
    }

    @Test
    void mapBuilderWithCodecReturnsEncodedPairOnSuccess() {
        MapBuilder builder = new MapBuilder();
        var result = getOrThrow(builder.with("n", 7, Codec.INT));

        assertEquals(7, result.getFirst().getAsInt());
        assertSame(builder, result.getSecond());

        JsonObject json = builder.build();
        assertEquals(0, json.size());
    }

    @Test
    void mapBuilderWithCodecReturnsErrorWhenEncodingFails() {
        Codec<Integer> failCodec = CodecUtil.of(
            Codec.INT,
            value -> TestedCodec.fail(() -> "always fail")
        );
        var result = new MapBuilder().with("n", 7, failCodec);
        assertTrue(isError(result));
        assertTrue(result.error().orElseThrow().message().contains("always fail"));
    }
}
