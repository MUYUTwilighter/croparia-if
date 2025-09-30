package cool.muyucloud.croparia.api.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

public record PatternKey(Pattern pattern) {
    public static final Pattern EMPTY = Pattern.compile("^$");
    public static final Pattern QUOTE_IF_STR = literal("_qis");
    public static final Pattern QUOTE = literal("_q");

    public static PatternKey of(Pattern pattern) {
        return new PatternKey(pattern);
    }

    public static PatternKey ofLiteral(String literal) {
        return new PatternKey(literal(literal));
    }

    public static Pattern literal(String literal) {
        if (literal.isEmpty()) return EMPTY;
        else return Pattern.compile(Pattern.quote(literal));
    }

    @Override
    public @NotNull String toString() {
        return pattern.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o instanceof Pattern oPattern) {
            return Objects.equals(this.pattern().pattern(), oPattern.pattern());
        } else if (o instanceof PatternKey(Pattern oPattern)) {
            return Objects.equals(this.pattern().pattern(), oPattern.pattern());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pattern().pattern());
    }
}
