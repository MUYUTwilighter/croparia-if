package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A builder for creating {@link Placeholder} instances with a fluent API.
 *
 * @param <T> The type of the entry that the placeholder will process.
 * @apiNote This builder allows you to define how to parse placeholders using regex patterns and delegate parsers.
 * It supports self-parsing, sub-node parsing, and provides utility methods for common data structures like maps and lists.
 * The built {@link Placeholder} can be used in data generation contexts to dynamically generate JSON based on the entry data.
 */
public class PlaceholderBuilder<T> {
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
    public static <T, V> PlaceholderBuilder<T> ofMap(TypeMapper<T, @NotNull MapReader<String, V>> mapper, Placeholder<V> valueParser) {
        return PlaceholderBuilder.of(parser -> parser.self((entry, placeholder, matcher) -> {
            if (entry instanceof JsonObject obj) {
                return Optional.of(obj);
            }
            JsonObject obj = new JsonObject();
            mapper.map(entry, placeholder, matcher).ifPresent(map -> map.forEach(
                e -> valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(v -> obj.add(e.getKey(), v))
            ));
            return Optional.of(obj);
        }).then(
            Pattern.compile("^size$|^length$"), (entry, placeholder, matcher) -> mapper.map(entry, placeholder, matcher).map(MapReader::size), Placeholder.NUMBER
        ).then(Pattern.compile("mapKey\\((.*)\\)"), (entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            mapper.map(entry, placeholder, matcher).ifPresent(map -> map.forEach(
                e -> valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(
                    k -> valueParser.parse(e.getValue(), "", PatternKey.EMPTY.matcher("")).ifPresent(
                        v -> obj.add(k.getAsString(), v)
                    )
                )
            ));
            return Optional.of(obj);
        }, Placeholder.JSON_OBJECT).then(Pattern.compile("^mapValue\\((.*)\\)$"), (entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            mapper.map(entry, placeholder, matcher).ifPresent(map -> map.forEach(
                e -> valueParser.parse(e.getValue(), placeholder, matcher).ifPresent(v -> obj.add(e.getKey(), v))
            ));
            return Optional.of(obj);
        }, Placeholder.JSON_OBJECT).then(PatternKey.literal("values()"), (entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            mapper.map(entry, placeholder, matcher).ifPresent(map -> map.values().forEach(value -> valueParser.parse(value, placeholder, matcher).ifPresent(array::add)));
            return Optional.of(array);
        }, Placeholder.JSON_ARRAY).then(PatternKey.literal("keys()"), (entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            mapper.map(entry, placeholder, matcher).ifPresent(map -> map.values().forEach(value -> valueParser.parse(value, placeholder, matcher).ifPresent(array::add)));
            return Optional.of(array);
        }, Placeholder.JSON_ARRAY).then(Pattern.compile("^get\\((.*)\\)$"), (entry, placeholder, matcher) -> {
            String key = matcher.group(1);
            return mapper.map(entry, placeholder, matcher).map(map -> map.get(key))
                .flatMap(value -> valueParser.parse(value, placeholder, matcher));
        }).then(Pattern.compile("^getOr\\((.*),(.*)\\)$"), (entry, placeholder, matcher) -> {
            String key = matcher.group(1);
            String def = matcher.group(2);
            return mapper.map(entry, placeholder, matcher).map(map -> map.get(key))
                .flatMap(value -> valueParser.parse(value, placeholder, matcher)).or(() -> Optional.of(new JsonPrimitive(def)));
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
    public static <T, E> PlaceholderBuilder<T> ofList(TypeMapper<T, ListReader<E>> mapper, Placeholder<E> elementParser) {
        return PlaceholderBuilder.of(parser -> parser.self((entry, placeholder, matcher) -> {
            if (entry instanceof JsonArray array) {
                return Optional.of(array);
            }
            JsonArray array = new JsonArray();
            mapper.map(entry, placeholder, matcher).ifPresent(
                list -> list.forEach(element -> elementParser.parse(element, placeholder, matcher).ifPresent(array::add))
            );
            return Optional.of(array);
        }).then(
            PatternKey.literal("_size"),
            (entry, placeholder, matcher) -> mapper.map(entry, placeholder, matcher).map(ListReader::size),
            Placeholder.NUMBER
        ).then(Pattern.compile("map\\(.*\\)"), (entry, placeholder, matcher) -> {
            JsonArray array = new JsonArray();
            mapper.map(entry, placeholder, matcher).ifPresent(
                list -> list.forEach(element -> elementParser.parse(element, placeholder, matcher).ifPresent(array::add))
            );
            return Optional.of(array);
        }, Placeholder.JSON_ARRAY).then(PatternKey.literal("mapi()"), (entry, placeholder, matcher) -> {
            JsonObject obj = new JsonObject();
            mapper.map(entry, placeholder, matcher).ifPresent(list -> {
                int i = 0;
                for (E e : list) {
                    String k = String.valueOf(i);
                    elementParser.parse(e, placeholder, matcher).ifPresent(v -> obj.add(k, v));
                    i++;
                }
            });
            return Optional.of(obj);
        }, Placeholder.JSON_OBJECT).then(Pattern.compile("^get\\(\\d+\\)$"), (entry, placeholder, matcher) -> {
            try {
                int index = Integer.parseInt(matcher.group(1));
                return mapper.map(entry, placeholder, matcher).map(
                    list -> list.get(index)
                ).flatMap(element -> elementParser.parse(element, placeholder, matcher));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                return Optional.empty();
            }
        }));
    }

    private final Map<PatternKey, RegexParser<T>> subNodes = new LinkedHashMap<>();

    public PlaceholderBuilder() {
        this.then(PatternKey.QUOTE_IF_STR, (entry, placeholder, matcher) -> {
            RegexParser<T> parser = this.subNodes.get(PatternKey.of(PatternKey.EMPTY));
            if (parser != null) {
                return parser.parse(entry, placeholder, matcher).map(json -> {
                    if (json.isJsonPrimitive()) {
                        JsonPrimitive primitive = json.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            return new JsonPrimitive(primitive.toString());
                        }
                    }
                    return json;
                });
            } else {
                return Optional.empty();
            }
        });
        this.then(PatternKey.QUOTE, (entry, placeholder, matcher) -> {
            RegexParser<T> parser = this.subNodes.get(PatternKey.of(PatternKey.EMPTY));
            if (parser != null) {
                return parser.parse(entry, placeholder, matcher).map(json -> {
                    if (json.isJsonPrimitive()) {
                        JsonPrimitive primitive = json.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            return new JsonPrimitive(primitive.toString());
                        }
                    }
                    return new JsonPrimitive("\"" + json + "\"");
                });
            } else {
                return Optional.empty();
            }
        });
    }

    @SuppressWarnings("unused")
    public PlaceholderBuilder(RegexParser<T> selfParser) {
        this.self(selfParser);
    }

    protected Map<PatternKey, RegexParser<T>> getSubNodes() {
        return subNodes;
    }

    /**
     * Encode the entry itself using the given codec when the placeholder is empty.
     *
     * @param parser The parser to use for the entry itself.
     * @return The current builder instance for chaining.
     */
    protected @NotNull PlaceholderBuilder<T> self(@NotNull RegexParser<T> parser) {
        return then(PatternKey.EMPTY, parser);
    }

    public @NotNull <F> PlaceholderBuilder<T> self(TypeMapper<T, F> mapper, @NotNull Placeholder<F> parser) {
        return then(PatternKey.EMPTY, mapper, parser);
    }

    public @NotNull <F> PlaceholderBuilder<T> self(TypeMapper<T, F> mapper, @NotNull Codec<F> codec) {
        return then(PatternKey.EMPTY, mapper, codec);
    }

    protected @NotNull PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull RegexParser<T> parser) {
        this.subNodes.put(PatternKey.of(key), parser);
        return this;
    }

    public @NotNull <O> PlaceholderBuilder<T> then(@NotNull Pattern key, TypeMapper<T, O> mapper, @NotNull Placeholder<O> parser) {
        return then(key, ((entry, placeholder, matcher) -> mapper.map(entry, placeholder, matcher)
            .flatMap(other -> parser.parse(other, placeholder, matcher))));
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into a type,
     * then encodes it into Json using the provided codec.
     *
     * @param key    The pattern to match the placeholder key.
     * @param mapper The function to convert the entry into a type.
     * @param codec  The codec to use for encoding the type into Json.
     * @param <O>    The type to convert the entry into.
     * @return The current builder instance for chaining.
     * @apiNote This is a shorthand for then(key, t -> CodecUtil.encodeJson(mapper.apply(t), codec).getOrThrow(PlaceholderException::new))
     */
    public @NotNull <O> PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull TypeMapper<T, O> mapper, @NotNull Codec<O> codec) {
        return then(key, (entry, placeholder, matcher) -> mapper.map(entry, placeholder, matcher).map(mapped -> {
            AtomicReference<JsonElement> ref = new AtomicReference<>();
            CodecUtil.encodeJson(mapped, codec).ifSuccess(ref::set);
            return ref.get();
        }), Placeholder.JSON);
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided parser to parse the entry.
     *
     * @param key    The pattern to match the placeholder key.
     * @param parser The parser to use for parsing the entry when the key matches.
     * @return The current builder instance for chaining.
     * @apiNote If you want to parse the entry into Json, you should use {@code then(key, mapper, Placeholder.JSON)} so
     * that the parser would support auto-casting and Json Querier.
     */
    public @NotNull PlaceholderBuilder<T> then(@NotNull Pattern key, @NotNull Placeholder<T> parser) {
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
     * @see #ofMap(TypeMapper, Placeholder)
     */
    public @NotNull <V> PlaceholderBuilder<T> thenMap(@NotNull Pattern key, @NotNull TypeMapper<T, MapReader<String, V>> mapper, @NotNull Placeholder<V> valueParser) {
        return this.then(key, ofMap(mapper, valueParser).build());
    }

    /**
     * Add a sub-node that matches the given key pattern and uses the provided mapper to convert the entry into a list,
     * then parses the elements using the provided element parser.
     */
    @SuppressWarnings("unused")
    public @NotNull <E> PlaceholderBuilder<T> thenList(@NotNull Pattern key, @NotNull TypeMapper<T, ListReader<E>> mapper, @NotNull Placeholder<E> elemParser) {
        return this.then(key, ofList(mapper, elemParser).build());
    }

    /**
     * Overwrite the sub-nodes of this builder with the sub-nodes of another placeholder.
     */
    public @NotNull <O> PlaceholderBuilder<T> overwrite(@NotNull Placeholder<O> other, @NotNull TypeMapper<T, O> mapper) {
        for (var entry : other.getSubNodes().entrySet()) {
            subNodes.put(entry.getKey(), (t, p, m) -> mapper.map(t, p, m).flatMap(o -> entry.getValue().parse(o, p, m)));
        }
        return this;
    }

    public @NotNull <O> PlaceholderBuilder<T> concat(@NotNull Placeholder<O> other, @NotNull TypeMapper<T, O> mapper) {
        for (var entry : other.getSubNodes().entrySet()) {
            this.subNodes.putIfAbsent(entry.getKey(), (t, s, m) -> mapper.map(t, s, m).flatMap(o -> entry.getValue().parse(o, s, m)));
        }
        return this;
    }

    public @NotNull PlaceholderBuilder<T> remove(@NotNull Pattern key) {
        this.subNodes.remove(new PatternKey(key));
        return this;
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
    public <O> PlaceholderBuilder<O> map(@NotNull TypeMapper<O, T> mapper) {
        PlaceholderBuilder<O> mapped = new PlaceholderBuilder<>();
        for (Map.Entry<PatternKey, RegexParser<T>> entry : this.subNodes.entrySet()) {
            PatternKey key = entry.getKey();
            RegexParser<T> parser = entry.getValue();
            mapped.subNodes.put(key, (oEntry, placeholder, matcher) -> mapper.map(oEntry, placeholder, matcher).flatMap(t -> parser.parse(t, placeholder, matcher)));
        }
        return mapped;
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
}
