package cool.muyucloud.croparia.api.json.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.*;

import java.io.StringReader;
import java.util.List;

public class JsonTransformerImpl {
    public static final TomlParser TOML_PARSER = new TomlParser();

    public static JsonElement transformToml(String raw) {
        try {
            CommentedConfig config = TOML_PARSER.parse(new StringReader(raw));
            return toJson(config);
        } catch (ParsingException e) {
            throw new JsonParseException(e);
        }
    }

    private static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof Config config) {
            JsonObject json = new JsonObject();
            for (Config.Entry entry : config.entrySet()) {
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
