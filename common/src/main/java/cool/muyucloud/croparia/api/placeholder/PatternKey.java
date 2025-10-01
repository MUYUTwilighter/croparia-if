package cool.muyucloud.croparia.api.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

public record PatternKey(Pattern pattern) {
    public static final Pattern EMPTY = Pattern.compile("^$");
    public static final Pattern QUOTE_IF_STR = literal("_qis");
    public static final Pattern QUOTE = literal("_q");
    public static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{(?s)(.+)}$");
    public static final Pattern MAP_MAP_KEY = Pattern.compile("mapKey\\(([^)]*)\\)");
    public static final Pattern MAP_MAP_VALUE = Pattern.compile("^mapValue\\(([^)]*)\\)$");
    public static final Pattern MAP_GET = Pattern.compile("^get\\(([^)]*)\\)$");
    public static final Pattern MAP_GET_OR = Pattern.compile("^getOr\\(([^,]*),([^)]*)\\)$");
    public static final Pattern LIST_MAP = Pattern.compile("map\\(([^)]*)\\)");
    public static final Pattern LIST_GET = Pattern.compile("^get\\((\\d+)\\)$");
    public static final Pattern LIST_GET_OR = Pattern.compile("^getOr\\((\\d+),([^)]*)\\)$");

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
            return Objects.equals(this.pattern().pattern(), oPattern.pattern()) && Objects.equals(this.pattern().flags(), oPattern.flags());
        } else if (o instanceof PatternKey(Pattern oPattern)) {
            return Objects.equals(this.pattern().pattern(), oPattern.pattern()) && Objects.equals(this.pattern().flags(), oPattern.flags());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern().pattern(), pattern().flags());
    }
}
