package cool.muyucloud.croparia.api.json.neoforge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTransformerImplNeoForgeTest {
    @Test
    void transformsToml() {
        JsonObject json = JsonTransformerImpl.transformToml("name = \"croparia\"").getAsJsonObject();
        assertEquals("croparia", json.get("name").getAsString());
    }

    @Test
    void invalidTomlThrowsJsonParseException() {
        assertThrows(JsonParseException.class, () -> JsonTransformerImpl.transformToml("name = "));
    }
}
