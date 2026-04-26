package cool.muyucloud.croparia.api.json.fabric;

import com.google.gson.JsonElement;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.serial.gson.GsonTomlSerializer;

public class JsonTransformerImpl {
    public static JsonElement transformToml(String raw) {
        return GsonTomlSerializer.instance().serialize(JToml.jToml().readFromString(raw));
    }
}
