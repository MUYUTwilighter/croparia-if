package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderTemplateTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    @Test
    void templateParsesSimplePlaceholders() {
        Placeholder<String> parser = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("len"), TypeMapper.of(String::length), Placeholder.NUMBER)
        );
        Template template = new Template("len=${len}; again=${len}");
        assertEquals("len=4; again=4", template.parse("muyu", parser));
    }

    @Test
    void templateKeepsEscapedPlaceholderLiteral() {
        Placeholder<String> parser = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("len"), TypeMapper.of(String::length), Placeholder.NUMBER)
        );
        Template template = new Template("\\${len} ${len}");
        assertEquals("\\${len} 4", template.parse("test", parser));
    }

    @Test
    void templateThrowsOnUnclosedPlaceholder() {
        assertThrows(JsonParseException.class, () -> new Template("${len"));
    }

    @Test
    void mapAndListPlaceholderHelpersWork() {
        Placeholder<Map<String, String>> mapParser =
            Placeholder.buildMap(TypeMapper.of(map -> MapReader.map(map)), Placeholder.STRING, builder -> builder);
        Placeholder<List<String>> listParser =
            Placeholder.buildList(TypeMapper.of(ListReader::list), Placeholder.STRING, builder -> builder);

        assertEquals("value", mapParser.parseStart(Map.of("a", "value"), "get(a)", matcherFor("get(a)")));
        assertEquals("fallback", mapParser.parseStart(Map.of(), "getOr(a,fallback)", matcherFor("getOr(a,fallback)")));
        assertEquals("${get(missing)}", mapParser.parseStart(Map.of(), "get(missing)", matcherFor("get(missing)")));

        assertEquals("y", listParser.parseStart(List.of("x", "y"), "get(1)", matcherFor("get(1)")));
        assertEquals("fallback", listParser.parseStart(List.of("x"), "getOr(2,fallback)", matcherFor("getOr(2,fallback)")));
    }

    @Test
    void noMatchingKeyErrorContainsUsefulContext() {
        Placeholder<String> parser = Placeholder.build(builder -> builder.self(RegexParser.of(entry -> entry)));
        PlaceholderException exception = assertThrows(
            PlaceholderException.class,
            () -> parser.parse("entry", "missing.path", matcherFor("missing.path"))
        );
        assertTrue(exception.getMessage().contains("segment 'missing'"));
        assertTrue(exception.getMessage().contains("remaining: 'path'"));
    }
}
