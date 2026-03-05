package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapReaderTest {
    @Test
    void mapReaderExposesEntriesKeysAndValues() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        MapReader<String, Integer> reader = MapReader.map(source);

        assertEquals(2, reader.size());
        assertEquals(1, reader.get("a"));
        assertEquals(List.of("a", "b"), reader.keys().stream().toList());
        assertEquals(List.of(1, 2), reader.values().stream().toList());
    }

    @Test
    void mappedReaderTransformsKeysAndUsesGetter() {
        MapReader<String, Integer> reader = MapReader.map(Map.of("x", 2, "y", 3));
        MapReader<String, Integer> mapped = reader.map(k -> "k-" + k, k2 -> k2.length());

        assertEquals(2, mapped.size());
        assertEquals(3, mapped.get("abc"));
        assertTrue(mapped.keys().contains("k-x"));
        assertTrue(mapped.keys().contains("k-y"));
        assertTrue(mapped.values().contains(3));
    }

    @Test
    void jsonObjectReaderReflectsJsonObjectContent() {
        JsonObject json = new JsonObject();
        json.addProperty("name", "croparia");
        json.addProperty("n", 5);

        MapReader<String, com.google.gson.JsonElement> reader = MapReader.json(json);
        assertEquals("croparia", reader.get("name").getAsString());
        assertEquals(5, reader.get("n").getAsInt());
        assertTrue(reader.keys().contains("name"));
        assertTrue(reader.values().contains(reader.get("n")));
    }
}
