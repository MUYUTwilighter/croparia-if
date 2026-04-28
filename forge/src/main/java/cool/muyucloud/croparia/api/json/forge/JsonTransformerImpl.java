package cool.muyucloud.croparia.api.json.forge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.*;

import java.util.List;
import java.util.Map;

public class JsonTransformerImpl {
    public static final TomlParser TOML_PARSER = new TomlParser();

    public static JsonElement transformToml(String raw) {
        return toJson(TOML_PARSER.parse(raw));
    }

    private static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof CommentedConfig commentedConfig) {
            return toJson(commentedConfig.valueMap());
        }
        if (value instanceof Config config) {
            return toJson(config.valueMap());
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject json = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                json.add(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            }
            return json;
        }
        if (value instanceof List<?> list) {
            JsonArray json = new JsonArray();
            for (Object element : list) {
                json.add(toJson(element));
            }
            return json;
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof Character character) {
            return new JsonPrimitive(character);
        }
        return new JsonPrimitive(String.valueOf(value));
    }
}
