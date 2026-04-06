package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonTransformerTest {
    @Test
    void transformsJsonByExtension() {
        JsonObject json = JsonTransformer.transform("{\"a\":1}", "sample.json").getAsJsonObject();
        assertEquals(1, json.get("a").getAsInt());
    }

    @Test
    void transformsTomlByExtension() {
        JsonObject json = JsonTransformer.transform("name = \"croparia\"", "sample.toml").getAsJsonObject();
        assertEquals("croparia", json.get("name").getAsString());
    }

    @Test
    void unknownExtensionFallsBackToJsonParser() {
        JsonObject json = JsonTransformer.transform("{\"k\":\"v\"}", "sample.unknown").getAsJsonObject();
        assertEquals("v", json.get("k").getAsString());
    }

    @Test
    void invalidTomlThrowsSyntaxException() {
        JsonSyntaxException exception = assertThrows(
            JsonSyntaxException.class,
            () -> JsonTransformer.transform("name = ", "broken.toml")
        );
        assertTrue(exception.getMessage().contains("Failed to parse TOML"));
    }
}
