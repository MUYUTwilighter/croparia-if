package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonElement;
import cool.muyucloud.croparia.api.json.JsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;

public interface RegexParser<T> {
    static <T> RegexParser<T> of() {
        return of(e -> e);
    }

    static <T> RegexParser<T> of(Direct<T> parser) {
        return parser.asNode();
    }

    static <T> RegexParser<T> of(Pass<T> parser) {
        return parser.asNode();
    }

    /**
     * Parse the placeholder.
     *
     * @param entry       The entry that provide the values.
     * @param placeholder The placeholder to be processed.
     * @param matcher     The matcher of the pattern that matched the placeholder.
     * @return The processed JsonElement, or Optional.empty() if the placeholder is not recognized.
     * @throws RegexParserException If any error occurs during processing.
     */
    Optional<JsonElement> parse(T entry, @NotNull String placeholder, @NotNull Matcher matcher) throws RegexParserException;

    static String forward(@NotNull String placeholder) {
        int i = placeholder.indexOf('.');
        if (i == -1) {
            return "";
        } else {
            return placeholder.length() > i + 1 ? placeholder.substring(i + 1) : "";
        }
    }

    static String next(@NotNull String placeholder) {
        int i = placeholder.indexOf('.');
        if (i == -1) {
            return placeholder;
        } else {
            return placeholder.substring(0, i);
        }
    }

    interface Direct<T> {
        Object process(@NotNull T entry) throws RegexParserException;

        default RegexParser<T> asNode() {
            return (entry, placeholder, matcher) -> Optional.ofNullable(JsonBuilder.parse(this.process(entry)));
        }
    }

    interface Pass<T> {
        Object process(@NotNull T entry, @NotNull String placeholder) throws RegexParserException;

        default RegexParser<T> asNode() {
            return (entry, placeholder, matcher) -> Optional.ofNullable(JsonBuilder.parse(this.process(entry, placeholder)));
        }
    }
}
