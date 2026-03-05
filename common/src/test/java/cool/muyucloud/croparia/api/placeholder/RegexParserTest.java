package cool.muyucloud.croparia.api.placeholder;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexParserTest {
    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    @Test
    void passParserCanUseForwardedPlaceholder() {
        RegexParser<String> parser = RegexParser.of((entry, placeholder) -> entry + ":" + placeholder);
        var parsed = parser.parse("x", "a.b", matcherFor("a.b")).orElseThrow();
        assertEquals("x:a.b", parsed.getAsString());
    }
}
