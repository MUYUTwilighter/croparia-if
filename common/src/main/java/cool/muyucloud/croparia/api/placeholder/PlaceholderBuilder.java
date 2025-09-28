package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.util.ListReader;
import cool.muyucloud.croparia.util.MapReader;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A builder for creating {@link Placeholder} instances with a fluent API.
 *
 * @param <T> The type of the entry that the placeholder will process.
 * @apiNote This builder allows you to define how to parse placeholders using regex patterns and delegate parsers.
 * It supports self-parsing, sub-node parsing, and provides utility methods for common data structures like maps and lists.
 * The built {@link Placeholder} can be used in data generation contexts to dynamically generate JSON based on the entry data.
 */
public class PlaceholderBuilder<T> implements RegexParser<T> {
    /**
     * Create an empty PlaceholderBuilder.
     *
     * @param <T> The type of the entry.
     * @return A new empty PlaceholderBuilder instance.
     */
    public static <T> PlaceholderBuilder<T> of() {
        return new PlaceholderBuilder<>();
    }

    /**
     * Create a PlaceholderBuilder using the provided delegate to configure it.
     *
     * @param delegate The consumer that configures the builder.
     * @param <T>      The type of the entry.
     * @return A new PlaceholderBuilder instance configured by the delegate.
     */
    public static <T> PlaceholderBuilder<T> of(Consumer<PlaceholderBuilder<T>> delegate) {
        PlaceholderBuilder<T> parser = new PlaceholderBuilder<>();
        delegate.accept(parser);
        return parser;
    }

    /**
     * Create a PlaceholderBuilder for a map type.
     *
     * @param mapper      The function to convert the entry into a map.
     * @param valueParser The parser to use for parsing the map values.
     * @param <T>         The type of the entry.
     * @param <V>         The type of the map values.
     * @return A new PlaceholderBuilder instance for the map type.
     * @apiNote The builder supports Json Object Querier.
     */
    public static <T, V> PlaceholderBuilder<T> ofMap(Function<T, MapReader<String, V>> mapper, RegexParser<V> valueParser) {
        return PlaceholderBuilder.of(parser -> parser.self((entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, V> e : mapper.apply(entry)) {
                valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(v -> obj.add(e.getKey(), v));
            }
            return Optional.of(obj);
        }).then(
            Pattern.compile("^size$|^length$"), RegexParser.of(entry -> mapper.apply(entry).size())
        ).then(Pattern.compile("^\\{}$"), (entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, V> e : mapper.apply(entry)) {
                valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(v -> obj.add(e.getKey(), v));
            }
            return Optional.of(obj);
        }).then(Pattern.compile("^\\[]$"), (entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            for (Map.Entry<String, V> e : mapper.apply(entry)) {
                valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(array::add);
            }
            return Optional.of(array);
        }).then(Pattern.compile("(^.*$)"), (entry, placeholder, matcher) -> {
            String key = matcher.group(1);
            V value = mapper.apply(entry).get(key);
            if (value == null) {
                throw new RegexParserException("Key not found: " + key);
            }
            return valueParser.parse(value, placeholder, matcher);
        }));
    }

    /**
     * Create a PlaceholderBuilder for a list type.
     *
     * @param mapper        The function to convert the entry into a list.
     * @param elementParser The parser to use for parsing the list elements.
     * @param <T>           The type of the entry.
     * @param <E>           The type of the list elements.
     * @return A new PlaceholderBuilder instance for the list type.
     * @apiNote The builder supports Json Array Querier.
     */
    public static <T, E> PlaceholderBuilder<T> ofList(Function<T, ListReader<E>> mapper, RegexParser<E> elementParser) {
        return PlaceholderBuilder.of(parser -> parser.self((entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            for (E e : mapper.apply(entry)) {
                elementParser.parse(e, placeholder, matcher).ifPresent(array::add);
            }
            return Optional.of(array);
        }).then(
            Pattern.compile("^size$|^length$"), RegexParser.of(entry -> mapper.apply(entry).size())
        ).then(Pattern.compile("\\[]"), (entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            for (E e : mapper.apply(entry)) {
                elementParser.parse(e, placeholder, matcher).ifPresent(array::add);
            }
            return Optional.of(array);
        }).then(Pattern.compile("^\\{}$"), (entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            int i = 0;
            for (E e : mapper.apply(entry)) {
                String k = String.valueOf(i);
                elementParser.parse(e, placeholder, matcher).ifPresent(v -> obj.add(k, v));
                i++;
            }
            return Optional.of(obj);
        }).then(Pattern.compile("(^\\d+$)"), (entry, placeholder, matcher) -> {
            try {
                int index = Integer.parseInt(matcher.group(1));
                E element = mapper.apply(entry).get(index);
                return elementParser.parse(element, placeholder, matcher);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                throw new RegexParserException("Invalid index: " + RegexParser.next(placeholder), e);
            }
        }));
    }

    private final Map<PatternKey, RegexParser<T>> subNodes = new LinkedHashMap<>();

    public PlaceholderBuilder() {
    }

    @SuppressWarnings("unused")
    public PlaceholderBuilder(RegexParser<T> selfParser) {
        this.self(selfParser);
    }

    /**
     * Encode the entry itself using the given codec when the placeholder is empty.
     *
     * @param parser The parser to use for the entry itself.
     * @return The current builder instance for chaining.
     */
    public @NotNull PlaceholderBuilder<T> self(@NotNull RegexParser<T> parser) {
        return then(Pattern.compile("^$"), parser);
    }

    /**
     * Encode the entry itself using the given codec when the placeholder is empty.
     *
     * @param codec The codec to use for encoding the entry into Json.
     * @return The current builder instance for chaining.
     * @apiNote <b>DO NOT</b> copy the {@link RegexParser} lambda here to {@link #then(Pattern, RegexParser)} or
     * {@link #then(Pattern, Function, RegexParser)} when you need to parse into Json for sub-parsing.<br/>
     * You should use {@link #then(Pattern, Function)} or {@code then(key, mapper, Placeholder.JSON)} so that the parser
     * would support auto-casting and Json Querier.
     */
    @SuppressWarnings("unused")
    public @NotNull PlaceholderBuilder<T> encodeSelf(@NotNull Codec<T> codec) {
        return then(Pattern.compile("^$"), (entry, placeholder, matcher) ->
            Optional.ofNullable(CodecUtil.encodeJson(entry, codec).getOrThrow(RegexParserException::new)));
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided parser to parse the entry.
     *
     * @param key    The pattern to match the placeholder key.
     * @param parser The parser to use for parsing the entry when the key matches.
     * @return The current builder instance for chaining.
     * @apiNote If you want to parse the entry into Json, you should use {@link #then(Pattern, Function)} or
     * {@code then(key, mapper, Placeholder.JSON)} so that the parser would support auto-casting and Json Querier.
     */
    public <N> @NotNull PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull Function<T, N> mapper, @NotNull RegexParser<N> parser) {
        return then(key, (entry, placeholder, matcher) -> {
            N next = mapper.apply(entry);
            if (next == null) return Optional.empty();
            return parser.parse(next, placeholder, matcher);
        });
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into Json.
     *
     * @param key    The pattern to match the placeholder key.
     * @param mapper The function to convert the entry into a JsonElement.
     * @return The current builder instance for chaining.
     * @apiNote This is a shorthand for {@code then(key, mapper, Placeholder.JSON)}.
     */
    public @NotNull PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull Function<T, JsonElement> mapper) {
        return then(key, mapper, Placeholder.JSON);
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into a type,
     * then encodes it into Json using the provided codec.
     *
     * @param key    The pattern to match the placeholder key.
     * @param mapper The function to convert the entry into a type.
     * @param codec  The codec to use for encoding the type into Json.
     * @param <N>    The type to convert the entry into.
     * @return The current builder instance for chaining.
     * @apiNote This is a shorthand for then(key, t -> CodecUtil.encodeJson(mapper.apply(t), codec).getOrThrow(RegexParserException::new))
     * @see #then(Pattern, Function)
     */
    public @NotNull <N> PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull Function<T, N> mapper, @NotNull Codec<N> codec) {
        return then(key, t -> CodecUtil.encodeJson(mapper.apply(t), codec).getOrThrow(RegexParserException::new));
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided parser to parse the entry.
     *
     * @param key    The pattern to match the placeholder key.
     * @param parser The parser to use for parsing the entry when the key matches.
     * @return The current builder instance for chaining.
     * @apiNote If you want to parse the entry into Json, you should use {@link #then(Pattern, Function)} or
     * {@code then(key, mapper, Placeholder.JSON)} so that the parser would support auto-casting and Json Querier.
     */
    public @NotNull PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull RegexParser<T> parser) {
        subNodes.put(new PatternKey(key), parser);
        return this;
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into a map,
     * then parses the values using the provided value parser.
     *
     * @param key         The pattern to match the placeholder key.
     * @param mapper      The function to convert the entry into a map.
     * @param valueParser The parser to use for parsing the map values.
     * @param <V>         The type of the map values.
     * @return The current builder instance for chaining.
     * @apiNote This is a shorthand for {@code then(key, mapper, ofMap(mapper, valueParser))}.
     * @see #ofMap(Function, RegexParser)
     */
    public @NotNull <V> PlaceholderBuilder<T> thenMap(@NotNull Pattern key, @NotNull Function<T, MapReader<String, V>> mapper, @NotNull RegexParser<V> valueParser) {
        return this.then(key, ofMap(mapper, valueParser));
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into a list,
     * then parses the elements using the provided element parser.
     *
     * @param key        The pattern to match the placeholder key.
     * @param mapper     The function to convert the entry into a list.
     * @param elemParser The parser to use for parsing the list elements.
     * @param <E>        The type of the list elements.
     * @return The current builder instance for chaining.
     * @apiNote This is a shorthand for {@code then(key, mapper, ofList(mapper, elemParser))}.
     * @see #ofList(Function, RegexParser)
     */
    @SuppressWarnings("unused")
    public @NotNull <E> PlaceholderBuilder<T> thenList(@NotNull Pattern key, @NotNull Function<T, ListReader<E>> mapper, @NotNull RegexParser<E> elemParser) {
        return this.then(key, ofList(mapper, elemParser));
    }

    /**
     * Concatenate another placeholder builder to this one, using the provided mapper to convert the entry type.
     *
     * @param other  The other placeholder builder to concatenate.
     * @param mapper The function to convert the entry type from T to O.
     * @param <O>    The other entry type.
     * @return The current builder instance for chaining.
     * @apiNote This method merges the sub-nodes of the other builder into this one.
     * If there are conflicting patterns, the other builder's patterns will overwrite this one's.
     */
    public @NotNull <O> PlaceholderBuilder<T> overwrite(@NotNull PlaceholderBuilder<O> other, @NotNull Function<T, O> mapper) {
        for (Map.Entry<PatternKey, RegexParser<O>> otherEntry : other.subNodes.entrySet()) {
            RegexParser<O> parser = otherEntry.getValue();
            subNodes.put(otherEntry.getKey(), (entry, placeholder, matcher) -> {
                O next = mapper.apply(entry);
                return parser.parse(next, placeholder, matcher);
            });
        }
        return this;
    }

    public @NotNull PlaceholderBuilder<T> overwrite(@NotNull PlaceholderBuilder<T> other) {
        return this.overwrite(other, Function.identity());
    }

    public @NotNull <O> PlaceholderBuilder<T> overwrite(@NotNull Placeholder<O> other, @NotNull Function<T, O> mapper) {
        return this.overwrite(other.copy(), mapper);
    }

    public @NotNull PlaceholderBuilder<T> overwrite(@NotNull Placeholder<T> other) {
        return this.overwrite(other, Function.identity());
    }

    /**
     * Similar to {@link #overwrite(PlaceholderBuilder, Function)}, but does not overwrite existing patterns.
     *
     * @param other  The other placeholder builder to concatenate.
     * @param mapper The function to convert the entry type from T to O.
     * @param <O>    The other entry type.
     * @return The current builder instance for chaining.
     * @apiNote This method merges the sub-nodes of the other builder into this one.
     * If there are conflicting patterns, the existing patterns in this builder will be preserved.
     */
    public @NotNull <O> PlaceholderBuilder<T> concat(@NotNull PlaceholderBuilder<O> other, @NotNull Function<T, O> mapper) {
        for (Map.Entry<PatternKey, RegexParser<O>> otherEntry : other.subNodes.entrySet()) {
            this.subNodes.putIfAbsent(otherEntry.getKey(), (entry, placeholder, matcher) -> {
                O next = mapper.apply(entry);
                return otherEntry.getValue().parse(next, placeholder, matcher);
            });
        }
        return this;
    }

    @SuppressWarnings("unused")
    public @NotNull PlaceholderBuilder<T> concat(@NotNull PlaceholderBuilder<T> other) {
        return this.concat(other, Function.identity());
    }

    public @NotNull <O> PlaceholderBuilder<T> concat(@NotNull Placeholder<O> other, @NotNull Function<T, O> mapper) {
        return this.concat(other.copy(), mapper);
    }

    @SuppressWarnings("unused")
    public @NotNull PlaceholderBuilder<T> concat(@NotNull Placeholder<T> other) {
        return this.concat(other, Function.identity());
    }

    /**
     * Map this placeholder builder to another type using the provided mapper function.
     * * @param mapper The function to convert the entry type from O to T.
     *
     * @param <O> The new entry type.
     * @return A new placeholder builder that maps from O to T using the provided mapper.
     * @apiNote This method creates a new placeholder builder that applies the mapper function to convert
     * the entry type from O to T before passing it to the original parsers.<br/>
     * The original builder remains unchanged.
     */
    public <O> PlaceholderBuilder<O> map(@NotNull Function<O, T> mapper) {
        PlaceholderBuilder<O> mapped = new PlaceholderBuilder<>();
        for (Map.Entry<PatternKey, RegexParser<T>> entry : this.subNodes.entrySet()) {
            PatternKey key = entry.getKey();
            RegexParser<T> parser = entry.getValue();
            mapped.subNodes.put(key, (oEntry, placeholder, matcher) -> {
                T next = mapper.apply(oEntry);
                return parser.parse(next, placeholder, matcher);
            });
        }
        return mapped;
    }

    /**
     * Create a copy of this placeholder builder.
     *
     * @return A new placeholder builder that is a copy of this one.
     * @apiNote The copy contains the same sub-nodes as the original, but is a separate instance.
     * Modifications to the copy will not affect the original, and vice versa.
     */
    public PlaceholderBuilder<T> copy() {
        PlaceholderBuilder<T> copy = new PlaceholderBuilder<>();
        copy.subNodes.putAll(this.subNodes);
        return copy;
    }

    /**
     * Build an immutable {@link Placeholder} from this builder.
     *
     * @return A new {@link Placeholder} instance containing the configuration of this builder.
     * @apiNote After calling this method, further modifications to the builder will not affect the
     * created {@link Placeholder}.
     */
    public Placeholder<T> build() {
        return new Placeholder<>(this);
    }

    /**
     * Analyze the placeholder and delegate to the appropriate sub-node parser.
     *
     * @param entry       The entry that provides the values.
     * @param placeholder The placeholder to be processed.
     * @param matcher     The matcher of the pattern that matched the placeholder.
     * @return The processed JsonElement, or Optional.empty() if the placeholder is not recognized.
     * @throws RegexParserException If any error occurs during processing.
     * @apiNote This method checks if the placeholder is empty, in which case it calls the self parser.<br/>
     * If not empty, it extracts the next key segment and looks for a matching sub-node parser.<br/>
     * If a matching sub-node is found, it forwards the remaining placeholder to that parser.<br/>
     * If no matching sub-node is found, it throws a RegexParserException.
     */
    @Override
    public Optional<JsonElement> parse(@NotNull T entry, @NotNull String placeholder, @NotNull Matcher matcher) throws RegexParserException {
        String forwarded = RegexParser.forward(placeholder);
        String next = RegexParser.next(placeholder);
        for (Map.Entry<PatternKey, RegexParser<T>> subEntry : subNodes.entrySet()) {
            Matcher subMatcher = subEntry.getKey().pattern.matcher(next);
            if (subMatcher.find()) {
                Optional<JsonElement> result = subEntry.getValue().parse(entry, forwarded, subMatcher);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        throw new RegexParserException("No matching key for: " + next);
    }

    public static class PatternKey {
        public static PatternKey of(Pattern pattern) {
            return new PatternKey(pattern);
        }

        private final Pattern pattern;

        public PatternKey(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return pattern.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (o instanceof Pattern oPattern) {
                return Objects.equals(this.pattern.pattern(), oPattern.pattern());
            } else if (o instanceof PatternKey key) {
                return Objects.equals(this.pattern.pattern(), key.pattern.pattern());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pattern.pattern());
        }
    }
}
