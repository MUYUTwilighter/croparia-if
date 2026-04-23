package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import cool.muyucloud.croparia.util.FileUtil;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.serial.gson.GsonTomlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public interface JsonTransformer {
    Map<String, JsonTransformer> TRANSFORMERS = new HashMap<>(Map.of(
        "json", JsonParser::parseString,
        "cdg", DgReader::read,
        "toml", raw -> GsonTomlSerializer.instance().serialize(JToml.jToml().read(new StringReader(raw)))
    ));

    static JsonElement transform(File file) throws IOException, JsonParseException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return transform(FileUtil.readUtf8(fis), file.getName());
        }
    }

    static JsonElement transform(String content, String filename) {
        String ext = FileUtil.extension(filename);
        JsonTransformer transformer = TRANSFORMERS.getOrDefault(ext, JsonParser::parseString);
        if (transformer == null) {
            throw new JsonParseException("No transformer found for extension: " + ext);
        }
        return transformer.transform(content);
    }

    JsonElement transform(String raw);
}
