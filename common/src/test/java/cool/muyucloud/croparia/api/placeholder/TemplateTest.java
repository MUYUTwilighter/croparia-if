package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateTest {
    @Test
    void supportsNestedBracesInsidePlaceholder() {
        Placeholder<String> parser = Placeholder.build(builder -> builder
            .self(RegexParser.of(entry -> entry))
            .then(PatternKey.literal("a{b}"), RegexParser.of(entry -> "ok:" + entry))
        );
        Template template = new Template("v=${a{b}}");
        assertEquals("v=ok:x", template.parse("x", parser));
    }

    @Test
    void cachesSamePlaceholderWithinSingleParse() {
        AtomicInteger invocationCount = new AtomicInteger(0);
        Placeholder<String> parser = Placeholder.build(builder -> builder
            .then(PatternKey.literal("id"), (entry, placeholder, matcher) -> {
                invocationCount.incrementAndGet();
                return RegexParser.of(e -> e).parse(entry, placeholder, matcher);
            })
        );

        Template template = new Template("${id}${id}");
        assertEquals("xx", template.parse("x", parser));
        assertEquals(1, invocationCount.get());
    }

    @Test
    void throwsForUnclosedPlaceholderAfterRead() {
        assertThrows(JsonParseException.class, () -> new Template("abc ${x"));
    }
}
