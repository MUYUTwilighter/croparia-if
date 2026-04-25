package cool.muyucloud.croparia.api.json.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.except.parse.TomlParseException;
import io.github.wasabithumb.jtoml.serial.gson.GsonTomlSerializer;

public class JsonTransformerImpl {
    public static JsonElement transformToml(String raw) {
        try {
            return GsonTomlSerializer.instance().serialize(JToml.jToml().readFromString(raw));
        } catch (TomlParseException e) {
            throw new JsonParseException(e);
        }
    }
}
