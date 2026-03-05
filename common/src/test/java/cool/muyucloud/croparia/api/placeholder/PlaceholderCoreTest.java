package cool.muyucloud.croparia.api.placeholder;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderCoreTest {
    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    @Test
    void regexParserNextAndForwardHandleSegments() {
        assertEquals("a", RegexParser.next("a.b.c"));
        assertEquals("b.c", RegexParser.forward("a.b.c"));
        assertEquals("single", RegexParser.next("single"));
        assertEquals("", RegexParser.forward("single"));
        assertEquals("", RegexParser.forward("tail."));
    }

    @Test
    void patternKeyEqualityMatchesPatternAndPatternKey() {
        PatternKey key = PatternKey.of(PatternKey.literal("abc"));
        assertTrue(key.equals(PatternKey.literal("abc")));
        assertTrue(key.equals(PatternKey.of(PatternKey.literal("abc"))));
        assertFalse(key.equals(PatternKey.literal("abcd")));
    }

    @Test
    void placeholderAccessDirectDelegatesToWrappedPlaceholder() {
        Placeholder<Map<String, String>> mapParser =
            Placeholder.buildMap(TypeMapper.of(map -> MapReader.map(map)), Placeholder.STRING, builder -> builder);
        PlaceholderAccess access = PlaceholderAccess.of(Map.of("k", "v"), mapParser);
        assertEquals("v", access.parsePlaceholder("get(k)", matcherFor("get(k)")));
    }

    @Test
    void listReaderIteratorThrowsWhenAdvancingPastEnd() {
        ListReader<String> listReader = ListReader.list(java.util.List.of("x"));
        var iterator = listReader.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("x", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
    }
}
