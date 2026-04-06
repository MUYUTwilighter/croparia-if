package cool.muyucloud.croparia.api.placeholder;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderAccessTest {
    private static Matcher matcherFor(String inner) {
        Matcher matcher = PatternKey.PLACEHOLDER.matcher("${" + inner + "}");
        assertTrue(matcher.matches());
        return matcher;
    }

    @Test
    void defaultParsePlaceholderWorksForMatchingPlaceholderAccess() {
        Placeholder<PlaceholderAccess> parser = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> "self"))
            .then(PatternKey.literal("id"), RegexParser.of(entry -> "ok"))
        );
        PlaceholderAccess access = new PlaceholderAccess() {
            @Override
            public Placeholder<?> placeholder() {
                return parser;
            }
        };

        assertEquals("ok", access.parsePlaceholder("id", matcherFor("id")));
    }

    @Test
    void defaultParsePlaceholderWrapsClassCastAsPlaceholderException() {
        PlaceholderAccess access = new PlaceholderAccess() {
            @Override
            public Placeholder<?> placeholder() {
                return Placeholder.NUMBER;
            }
        };

        assertThrows(PlaceholderException.class, () -> access.parsePlaceholder("", matcherFor("x")));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void directParsePlaceholderWrapsClassCastAsPlaceholderException() {
        Placeholder wrong = Placeholder.NUMBER;
        PlaceholderAccess.Direct direct = PlaceholderAccess.of("not-number", wrong);
        assertThrows(PlaceholderException.class, () -> direct.parsePlaceholder("", matcherFor("x")));
    }
}
