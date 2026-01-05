package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import cool.muyucloud.croparia.util.FileUtil;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface JsonTransformer {
    Map<String, JsonTransformer> TRANSFORMERS = new HashMap<>(Map.of(
        "json", JsonParser::parseString,
        "cdg", DgReader::read,
        "toml", raw -> {
            TomlParseResult toml = Toml.parse(raw);
            if (toml.hasErrors()) {
                throw new JsonSyntaxException("Failed to parse TOML: " + toml.errors());
            }
            return JsonParser.parseString(toml.toJson());
        }
    ));

    static JsonElement transform(File file) throws IOException, JsonParseException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = fis.readAllBytes();
            String content = new String(data);
            return transform(content, file.getName());
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
