package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JsonTransformerTest {
    @BeforeAll
    static void registerTomlTransformer() {
        try {
            Class<?> jTomlClass = Class.forName("io.github.wasabithumb.jtoml.JToml");
            Class<?> serializerClass = Class.forName("io.github.wasabithumb.jtoml.serial.gson.GsonTomlSerializer");
            Object serializer = serializerClass.getMethod("instance").invoke(null);
            Object jToml = jTomlClass.getMethod("jToml").invoke(null);
            JsonTransformer.TRANSFORMERS.put("toml", raw -> {
                try {
                    Object toml = jTomlClass.getMethod("readFromString", String.class).invoke(jToml, raw);
                    return (JsonObject) serializerClass.getMethod("serialize", Class.forName("io.github.wasabithumb.jtoml.value.TomlValue"))
                        .invoke(serializer, toml);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Test
    void transformsJsonByExtension() {
        JsonObject json = JsonTransformer.transform("{\"a\":1}", "sample.json").getAsJsonObject();
        assertEquals(1, json.get("a").getAsInt());
    }

    @Test
    void transformsTomlByExtension() {
        assumeTrue(JsonTransformer.TRANSFORMERS.containsKey("toml"));
        JsonObject json = JsonTransformer.transform("name = \"croparia\"", "sample.toml").getAsJsonObject();
        assertEquals("croparia", json.get("name").getAsString());
    }

    @Test
    void preservesMultilineTomlStrings() {
        assumeTrue(JsonTransformer.TRANSFORMERS.containsKey("toml"));
        JsonObject json = JsonTransformer.transform("""
            path = "croparia/blockstates/test.json"
            template = \"\"\"
            {
              "variants": {
                "": {
                  "model": "croparia:block/gourd"
                }
              }
            }
            \"\"\"
            """, "sample.toml").getAsJsonObject();
        assertTrue(json.get("template").getAsString().contains("\"model\": \"croparia:block/gourd\""));
    }

    @Test
    void unknownExtensionFallsBackToJsonParser() {
        JsonObject json = JsonTransformer.transform("{\"k\":\"v\"}", "sample.unknown").getAsJsonObject();
        assertEquals("v", json.get("k").getAsString());
    }

    @Test
    void invalidTomlThrowsSyntaxException() {
        assumeTrue(JsonTransformer.TRANSFORMERS.containsKey("toml"));
        JsonSyntaxException exception = assertThrows(
            JsonSyntaxException.class,
            () -> JsonTransformer.transform("name = ", "broken.toml")
        );
        assertTrue(exception.getMessage().contains("Failed to parse TOML"));
    }
}
