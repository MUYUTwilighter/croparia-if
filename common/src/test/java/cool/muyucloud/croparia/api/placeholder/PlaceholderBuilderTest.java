package cool.muyucloud.croparia.api.placeholder;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderBuilderTest {
    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    private static Placeholder<String> stringWithLen() {
        return Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("len"), TypeMapper.of(String::length), Placeholder.NUMBER)
        );
    }

    @Test
    void mapAndListHelpersSupportNestedQueries() {
        Placeholder<Map<String, String>> mapParser =
            Placeholder.buildMap(TypeMapper.of(map -> MapReader.map(map)), stringWithLen(), builder -> builder);
        Placeholder<List<String>> listParser =
            Placeholder.buildList(TypeMapper.of(ListReader::list), stringWithLen(), builder -> builder);

        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("a", "x");
        ordered.put("b", "yy");

        assertEquals("2", mapParser.parseStart(ordered, "_size", matcherFor("_size")));
        assertEquals("a", mapParser.parseStart(ordered, "keys().get(0)", matcherFor("keys().get(0)")));
        assertEquals("yy", mapParser.parseStart(ordered, "values().get(1)", matcherFor("values().get(1)")));

        String mappedKey = mapParser.parseStart(ordered, "mapKey(len)", matcherFor("mapKey(len)"));
        assertTrue(mappedKey.contains("\"1\""));
        assertTrue(mappedKey.contains("\"2\""));

        String mappedValue = mapParser.parseStart(ordered, "mapValue(len)", matcherFor("mapValue(len)"));
        assertTrue(mappedValue.contains("\"a\""));
        assertTrue(mappedValue.contains("\"b\""));

        assertEquals("2", listParser.parseStart(List.of("x", "yy"), "map(len).get(1)", matcherFor("map(len).get(1)")));
        assertEquals("yy", listParser.parseStart(List.of("x", "yy"), "mapi(len).get(1)", matcherFor("mapi(len).get(1)")));
    }

    @Test
    void getOrAndMissingPathsFollowFallbackRules() {
        Placeholder<Map<String, String>> mapParser =
            Placeholder.buildMap(TypeMapper.of(map -> MapReader.map(map)), Placeholder.STRING, builder -> builder);
        Placeholder<List<String>> listParser =
            Placeholder.buildList(TypeMapper.of(ListReader::list), Placeholder.STRING, builder -> builder);

        assertEquals("fallback", mapParser.parseStart(Map.of(), "getOr(a,fallback)", matcherFor("getOr(a,fallback)")));
        assertEquals("${get(a)}", mapParser.parseStart(Map.of(), "get(a)", matcherFor("get(a)")));
        assertEquals("fallback", listParser.parseStart(List.of("x"), "getOr(9,fallback)", matcherFor("getOr(9,fallback)")));
        assertEquals("${get(9)}", listParser.parseStart(List.of("x"), "get(9)", matcherFor("get(9)")));
    }

    @Test
    void invalidAndUnknownListQueriesDoNotCrashParseStart() {
        Placeholder<List<String>> listParser =
            Placeholder.buildList(TypeMapper.of(ListReader::list), Placeholder.STRING, builder -> builder);

        String overflowIndex = "get(999999999999999999999999999)";
        assertEquals("${" + overflowIndex + "}", listParser.parseStart(List.of("x"), overflowIndex, matcherFor(overflowIndex)));
        assertEquals("${unknown(1)}", listParser.parseStart(List.of("x"), "unknown(1)", matcherFor("unknown(1)")));
    }

    @Test
    void parseThrowsMeaningfulErrorForUnknownSegment() {
        Placeholder<String> parser = Placeholder.build(builder -> builder.self(RegexParser.of(entry -> entry)));
        PlaceholderException exception = assertThrows(
            PlaceholderException.class,
            () -> parser.parse("entry", "missing.path", matcherFor("missing.path"))
        );
        assertTrue(exception.getMessage().contains("segment 'missing'"));
        assertTrue(exception.getMessage().contains("remaining: 'path'"));
    }

    @Test
    void quoteHelpersFollowStringAndNonStringRules() {
        Placeholder<String> stringParser = Placeholder.build(builder -> builder.self(RegexParser.of(entry -> entry)));
        Placeholder<Number> numberParser = Placeholder.build(builder -> builder.self(RegexParser.of(entry -> entry)));

        assertEquals("\"abc\"", stringParser.parseStart("abc", "_q", matcherFor("_q")));
        assertEquals("\"abc\"", stringParser.parseStart("abc", "_qis", matcherFor("_qis")));

        assertEquals("\"12\"", numberParser.parseStart(12, "_q", matcherFor("_q")));
        assertEquals("12", numberParser.parseStart(12, "_qis", matcherFor("_qis")));
    }

    @Test
    void removeConcatOverwriteAndMapBehaveAsExpected() {
        PlaceholderBuilder<String> baseBuilder = PlaceholderBuilder.<String>of()
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("a"), RegexParser.of(entry -> "base-a"));
        PlaceholderBuilder<String> otherBuilder = PlaceholderBuilder.<String>of()
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("a"), RegexParser.of(entry -> "other-a"))
            .then(PatternKey.literal("b"), RegexParser.of(entry -> "other-b"));

        Placeholder<String> base = baseBuilder.build();
        Placeholder<String> other = otherBuilder.build();

        Placeholder<String> removed = base.toBuilder().remove(PatternKey.literal("a")).build();
        assertEquals("${a}", removed.parseStart("x", "a", matcherFor("a")));

        Placeholder<String> concat = base.toBuilder().concat(other, TypeMapper.identity()).build();
        assertEquals("base-a", concat.parseStart("x", "a", matcherFor("a")));
        assertEquals("other-b", concat.parseStart("x", "b", matcherFor("b")));

        Placeholder<String> overwritten = base.toBuilder().overwrite(other, TypeMapper.identity()).build();
        assertEquals("other-a", overwritten.parseStart("x", "a", matcherFor("a")));

        Placeholder<Integer> mapped = base.map(TypeMapper.of((Integer i) -> String.valueOf(i)));
        assertEquals("123", mapped.parseStart(123, "", matcherFor("a")));
    }

    @Test
    void codecAndMapperHelpersEncodeExpectedValues() {
        Placeholder<Integer> parser = Placeholder.build(builder -> builder
            .self(TypeMapper.identity(), com.mojang.serialization.Codec.INT)
            .then(PatternKey.literal("plus"), TypeMapper.of((Integer i) -> i + 1), com.mojang.serialization.Codec.INT)
        );
        assertEquals("7", parser.parseStart(7, "", matcherFor("x")));
        assertEquals("8", parser.parseStart(7, "plus", matcherFor("plus")));
    }

    @Test
    void thenMapAndThenListHelpersDelegateToMapAndListParsers() {
        record Entry(Map<String, String> map, List<String> list) {}
        Placeholder<Entry> parser = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> "entry"))
            .thenMap(PatternKey.literal("m"), TypeMapper.of(entry -> MapReader.map(entry.map())), Placeholder.STRING)
            .thenList(PatternKey.literal("l"), TypeMapper.of(entry -> ListReader.list(entry.list())), Placeholder.STRING)
        );
        Entry entry = new Entry(Map.of("k", "v"), List.of("a", "b"));

        assertEquals("v", parser.parseStart(entry, "m.get(k)", matcherFor("m.get(k)")));
        assertEquals("b", parser.parseStart(entry, "l.get(1)", matcherFor("l.get(1)")));
    }

    @Test
    void mappedPlaceholderFallsBackWhenMapperReturnsEmpty() {
        Placeholder<String> base = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("len"), TypeMapper.of(String::length), Placeholder.NUMBER)
        );
        Placeholder<Integer> mapped = base.map((entry, placeholder, matcher) -> java.util.Optional.empty());

        assertEquals("${len}", mapped.parseStart(1, "len", matcherFor("len")));
    }
}
