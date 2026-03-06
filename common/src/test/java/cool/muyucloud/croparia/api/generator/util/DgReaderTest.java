package cool.muyucloud.croparia.api.generator.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DgReaderTest {
    @Test
    void parsesMetaAndTemplateFromCdgSource() throws CdgFormatException {
        String source = """
            @type="croparia:generator";
            @enabled=true;
            hello ${id}
            """;

        JsonObject json = DgReader.read(source);
        assertEquals("croparia:generator", json.get("type").getAsString());
        assertTrue(json.get("enabled").getAsBoolean());
        assertEquals("hello ${id}", json.get("template").getAsString());
    }

    @Test
    void supportsTripleQuotedAndJsonValues() throws CdgFormatException {
        String source = """
            @content='''line1
            line2''';
            @meta={"a":[1,2],"ok":true};
            """;

        JsonObject json = DgReader.read(source);
        assertEquals("line1\nline2", json.get("content").getAsString());
        assertEquals(2, json.getAsJsonObject("meta").getAsJsonArray("a").size());
        assertTrue(json.getAsJsonObject("meta").get("ok").getAsBoolean());
    }

    @Test
    void supportsEscapedCharactersInQuotedString() throws CdgFormatException {
        JsonObject json = DgReader.read("@text=\"line1\\nline2\\t\\\\\";");
        assertEquals("line1\nline2\t\\", json.get("text").getAsString());
    }

    @Test
    void throwsWhenSemicolonIsMissing() {
        CdgFormatException exception = assertThrows(CdgFormatException.class, () -> DgReader.read("@x=1"));
        assertTrue(exception.getMessage().contains("Semicolon ';' not found"));
    }

    @Test
    void throwsWhenQuotesAreUnclosed() {
        CdgFormatException exception = assertThrows(CdgFormatException.class, () -> DgReader.read("@x=\"abc;"));
        assertTrue(exception.getMessage().contains("Unclosed quotes"));
    }

    @Test
    void readFileRequiresJsonObject() throws IOException {
        Path tempFile = Files.createTempFile("croparia-dgreader", ".json");
        try {
            Files.writeString(tempFile, "[]", StandardCharsets.UTF_8);
            assertThrows(JsonParseException.class, () -> DgReader.read(tempFile.toFile()));

            Files.writeString(tempFile, "{\"k\":1}", StandardCharsets.UTF_8);
            JsonObject json = DgReader.read(tempFile.toFile());
            assertEquals(1, json.get("k").getAsInt());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
