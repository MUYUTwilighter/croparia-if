package cool.muyucloud.croparia.reflection;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;

import static cool.muyucloud.croparia.TestSupport.getOrThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecordCodecBuilderReflectionTest {
    private record Sample(int value) {}

    @Test
    void getterEncoderAndDecoderCanBeReadFromBuilder() {
        RecordCodecBuilder<Sample, Integer> builder = Codec.INT.fieldOf("value").forGetter(Sample::value);

        var getter = RecordCodecBuilderReflection.getGetter(builder);
        var encoder = RecordCodecBuilderReflection.getEncoder(builder, new Sample(9));
        var decoder = RecordCodecBuilderReflection.getDecoder(builder);

        assertEquals(9, getter.apply(new Sample(9)));
        assertNotNull(encoder);
        assertNotNull(decoder);

        JsonObject json = new JsonObject();
        json.addProperty("value", 11);
        assertEquals(11, getOrThrow(decoder.decode(JsonOps.INSTANCE, getOrThrow(JsonOps.INSTANCE.getMap(json)))));
    }
}
