package cool.muyucloud.croparia.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.*;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.json.JsonTransformer;
import net.neoforged.fml.common.Mod;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

@Mod(CropariaIf.MOD_ID)
public class CropariaIfNeoForge {
    public static final TomlParser TOML_PARSER = new TomlParser();
    public CropariaIfNeoForge() {
        JsonTransformer.TRANSFORMERS.put("toml", raw -> {
            CommentedConfig config = TOML_PARSER.parse(new StringReader(raw));
            return toJson(config);
        });
        CropariaIf.init();
    }

    private static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof CommentedConfig commentedConfig) {
            return toJson(commentedConfig.entrySet());
        }
        if (value instanceof Config config) {
            return toJson(config.entrySet());
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
