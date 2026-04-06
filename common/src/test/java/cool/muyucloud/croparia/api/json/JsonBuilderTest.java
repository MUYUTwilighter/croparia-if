package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonBuilderTest {
    @Test
    void parsesPrimitiveValuesIntoJson() {
        assertEquals(1, JsonBuilder.parse(1).getAsInt());
        assertEquals("x", JsonBuilder.parse("x").getAsString());
        assertTrue(JsonBuilder.parse(true).getAsBoolean());
        assertEquals('a', JsonBuilder.parse('a').getAsCharacter());
    }

    @Test
    void mapAndListBuildersComposeNestedJson() {
        JsonObject map = JsonBuilder.map(
            "name", "croparia",
            "nested", JsonBuilder.list(1, 2, 3)
        );
        assertEquals("croparia", map.get("name").getAsString());
        JsonArray nested = map.getAsJsonArray("nested");
        assertEquals(3, nested.size());
        assertEquals(2, nested.get(1).getAsInt());
    }

    @Test
    void mapBuilderWithMethodsOverwriteAndAppend() {
        JsonObject result = new MapBuilder("a", 1)
            .with("a", 2)
            .with("b", "x")
            .with("c", new JsonPrimitive(true))
            .build();

        assertEquals(2, result.get("a").getAsInt());
        assertEquals("x", result.get("b").getAsString());
        assertTrue(result.get("c").getAsBoolean());
    }

    @Test
    void mapBuilderConstructsFromMap() {
        JsonObject result = JsonBuilder.map(Map.of("k", "v", "n", 5));
        assertEquals("v", result.get("k").getAsString());
        assertEquals(5, result.get("n").getAsInt());
    }

    @Test
    void unsupportedTypeThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> JsonBuilder.parse(new Object())
        );
        assertTrue(exception.getMessage().contains("Codec is required"));
    }
}
