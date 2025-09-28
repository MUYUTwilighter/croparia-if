package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.util.ListReader;
import cool.muyucloud.croparia.util.MapReader;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A placeholder parser that can parse placeholders in the format of "field.sublist.[].subfield".<br/>
 * The parser is built using a {@link PlaceholderBuilder}, which provides a fluent API to define how to parse each part of the placeholder.
 *
 * @apiNote This can be seen as an immutable version of {@link PlaceholderBuilder}, which can be reused and shared.<br/>
 * To create a new parser, use {@link #build(Function)} or {@link #build(Codec, Function)}.
 * @see PlaceholderBuilder
 */
public class Placeholder<T> implements RegexParser<T> {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Placeholder<String> STRING = Placeholder.build(node -> node.self(RegexParser.of(JsonPrimitive::new)));
    public static final Placeholder<Number> NUMBER = Placeholder.build(node -> node.self(RegexParser.of(JsonPrimitive::new)));
    @SuppressWarnings("unused")
    public static final Placeholder<Boolean> BOOLEAN = Placeholder.build(node -> node.self(RegexParser.of(JsonPrimitive::new)));
    public static final Placeholder<JsonElement> JSON = Placeholder.build(builder -> {
        builder.self(RegexParser.of());
        builder.then(Pattern.compile("(^.*$)"), (entry, placeholder, matcher) -> {
            if (entry.isJsonArray()) {
                return Placeholder.JSON_ARRAY.parse(entry.getAsJsonArray(), matcher.group(1) + "." + placeholder, matcher);
            } else if (entry.isJsonObject()) {
                return Placeholder.JSON_OBJECT.parse(entry.getAsJsonObject(), matcher.group(1) + "." + placeholder, matcher);
            } else {
                return Optional.empty();    // Pass to next parser
            }
        });
        return builder;
    });
    public static final Placeholder<JsonObject> JSON_OBJECT = Placeholder.build(node -> node
        .self(RegexParser.of())
        .concat(PlaceholderBuilder.ofMap(MapReader::jsonObject, Placeholder.JSON), Function.identity()));
    public static final Placeholder<JsonArray> JSON_ARRAY = Placeholder.build(node -> node
        .self(RegexParser.of())
        .concat(PlaceholderBuilder.ofList(ListReader::jsonArray, Placeholder.JSON), Function.identity()));
    public static final Placeholder<ResourceLocation> ID = Placeholder.build(node -> node
        .self(RegexParser.of(ResourceLocation::toString))
        .then(literal("namespace"), RegexParser.of(ResourceLocation::getNamespace))
        .then(literal("path"), RegexParser.of(ResourceLocation::getPath))
    );
    public static final Placeholder<BlockOutput> BLOCK_OUTPUT = build(BlockOutput.CODEC_COMP.codec(), builder -> builder
        .then(literal("id"), BlockOutput::getId, ID)
    );
    public static final Placeholder<ItemOutput> ITEM_OUTPUT = build(
        ItemOutput.CODEC_COMP.codec(), builder -> builder
            .then(literal("id"), ItemOutput::getId, ID)
            .then(literal("stack"), ItemOutput::createStack, ItemStack.CODEC)
    );
    public static final Placeholder<Item> ITEM = ID.map(Item::arch$registryName);
    public static final Placeholder<ItemStack> ITEM_STACK = ITEM_OUTPUT.map(ItemOutput::of);
    public static final Placeholder<Block> BLOCK = ID.map(Block::arch$registryName);
    @SuppressWarnings("unused")
    public static final Placeholder<BlockState> BLOCK_STATE = BLOCK_OUTPUT.map(BlockOutput::of);
    public static final Placeholder<DataComponentPatch> DATA_COMPONENTS = Placeholder.build(DataComponentPatch.CODEC, Function.identity());

    public static Pattern literal(String literal) {
        return Pattern.compile(Pattern.quote(literal));
    }

    /**
     * Build a placeholder parser using the given factory function to configure the root node.
     *
     * @param factory The factory function to configure the root node.
     * @param <T>     The type of the entry that provides the values.
     * @return A new placeholder parser.
     * @see #Placeholder(Function)
     */
    public static <T> Placeholder<T> build(Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return new Placeholder<>(factory);
    }

    /**
     * Build a placeholder parser using the given codec to encode the entry to JSON, and the given factory function to configure the root node.
     *
     * @param codec   The codec to encode the entry to JSON.
     * @param factory The factory function to configure the root node.
     * @param <T>     The type of the entry that provides the values.
     * @return A new placeholder parser.
     * @apiNote This is a shortcut for {@code build(builder -> builder.self(codec).then(...))}.
     * @see #Placeholder(Codec, Function)
     */
    public static <T> Placeholder<T> build(Codec<T> codec, Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return new Placeholder<>(codec, factory);
    }

    private final PlaceholderBuilder<T> root;

    public Placeholder(Codec<T> codec, Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        PlaceholderBuilder<T> builder = PlaceholderBuilder.of();
        factory.apply(builder);
        Placeholder<T> placeholder = JSON.map(t -> CodecUtil.encodeJson(t, codec).getOrThrow(RegexParserException::new));
        this.root = placeholder.root.overwrite(builder);
    }

    public Placeholder(Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        this(factory.apply(PlaceholderBuilder.of()));
    }

    public Placeholder(PlaceholderBuilder<T> root) {
        this.root = root;
    }

    /**
     * Create a mutable copy of this parser.
     *
     * @return A new {@link PlaceholderBuilder} that is a copy of this parser.
     */
    public PlaceholderBuilder<T> copy() {
        return this.root.copy();
    }

    /**
     * Parse the placeholder.
     *
     * @param entry       The entry that provide the values.
     * @param placeholder The placeholder to be processed.
     * @param matcher     The matcher of the pattern that matched the placeholder.
     * @return The processed JsonElement, or Optional.empty() if the placeholder is not recognized.
     * @throws RegexParserException If any error occurs during processing.
     * @see PlaceholderBuilder#parse(Object, String, Matcher)
     */
    public Optional<JsonElement> parse(T entry, @NotNull String placeholder, @NotNull Matcher matcher) {
        return this.root.parse(entry, placeholder, matcher);
    }

    /**
     * Create a new parser that maps the entry type using the given mapper function.
     *
     * @param mapper The function to map the entry type.
     * @param <O>    The new entry type.
     * @return A new parser that maps the entry type.
     */
    public <O> Placeholder<O> map(Function<O, T> mapper) {
        return new Placeholder<>(this.root.map(mapper));
    }

    public String parseStart(T entry, String placeholder, Matcher matcher) {
        try {
            JsonElement json = root.parse(entry, placeholder, matcher).orElseThrow(
                () -> new RegexParserException("Unrecognized placeholder: " + placeholder)
            );
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                return json.getAsString();
            } else {
                return json.toString();
            }
        } catch (RegexParserException e) {
            LOGGER.error("Error processing placeholder: " + placeholder, e);
        }
        return "${" + placeholder + "}";
    }
}
