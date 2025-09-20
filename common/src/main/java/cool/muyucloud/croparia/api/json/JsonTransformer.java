package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public interface JsonTransformer {
    Map<String, JsonTransformer> TRANSFORMERS = new HashMap<>(Map.of(
        "json", JsonParser::parseString,
        "cdg", DgReader::read,
        "toml", raw -> {
            TomlParseResult toml = Toml.parse(raw);
            if (toml.hasErrors()) {
                throw new IllegalArgumentException("Failed to parse TOML: " + toml.errors());
            }
            return JsonParser.parseString(toml.toJson());
        }
    ));

    static JsonElement transform(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == name.length() - 1) {
            throw new IllegalArgumentException("File must have an extension: " + name);
        }
        String ext = name.substring(dotIndex + 1).toLowerCase();
        JsonTransformer transformer = TRANSFORMERS.get(ext);
        if (transformer == null) {
            throw new IllegalArgumentException("No transformer found for extension: " + ext);
        }
        try (FileInputStream stream = new FileInputStream(file)) {
            return transformer.transform(new String(stream.readAllBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + name, e);
        }
    }

    JsonElement transform(String raw);
}
