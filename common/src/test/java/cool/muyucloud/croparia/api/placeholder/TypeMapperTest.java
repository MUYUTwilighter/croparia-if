package cool.muyucloud.croparia.api.placeholder;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class TypeMapperTest {
    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    @Test
    void identityReturnsEntryWhenNotNull() {
        TypeMapper<String, String> mapper = TypeMapper.identity();
        assertEquals("x", mapper.map("x", "ignored", matcherFor("ignored")).orElseThrow());
        assertFalse(mapper.map(null, "ignored", matcherFor("ignored")).isPresent());
    }

    @Test
    void functionMapperUsesEntryOnly() {
        TypeMapper<String, Integer> mapper = TypeMapper.of(String::length);
        assertEquals(4, mapper.map("muyu", "ignored", matcherFor("ignored")).orElseThrow());
    }

    @Test
    void biFunctionMapperCanUsePlaceholderText() {
        TypeMapper<String, String> mapper = TypeMapper.of((entry, placeholder) -> entry + ":" + placeholder);
        assertEquals("a:b.c", mapper.map("a", "b.c", matcherFor("b.c")).orElseThrow());
    }

    @Test
    void function3MapperCanUseMatcherGroups() {
        TypeMapper<String, String> mapper = TypeMapper.of((entry, placeholder, matcher) -> entry + ":" + matcher.group(1));
        assertEquals("x:key", mapper.map("x", "key", matcherFor("key")).orElseThrow());
    }
}
