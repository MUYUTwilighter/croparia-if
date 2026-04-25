package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonTransformerTest {
    @Test
    void transformsJsonByExtension() {
        JsonObject json = JsonTransformer.transform("{\"a\":1}", "sample.json").getAsJsonObject();
        assertEquals(1, json.get("a").getAsInt());
    }

    @Test
    void dispatchesTomlByExtension() {
        JsonTransformer original = JsonTransformer.TRANSFORMERS.get("toml");
        JsonTransformer.TRANSFORMERS.put("toml", raw -> new JsonPrimitive("toml:" + raw));
        try {
            assertEquals("toml:name", JsonTransformer.transform("name", "sample.toml").getAsString());
        } finally {
            JsonTransformer.TRANSFORMERS.put("toml", original);
        }
    }

    @Test
    void unknownExtensionFallsBackToJsonParser() {
        JsonObject json = JsonTransformer.transform("{\"k\":\"v\"}", "sample.unknown").getAsJsonObject();
        assertEquals("v", json.get("k").getAsString());
    }

}
