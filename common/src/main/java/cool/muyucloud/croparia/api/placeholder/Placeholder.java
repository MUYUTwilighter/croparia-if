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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A placeholder parser that can parse placeholders in the format of "field.sublist.[].subfield".<br/>
 * The parser is built using a {@link PlaceholderBuilder}, which provides a fluent API to define how to parse each part of the placeholder.
 *
 * @apiNote This can be seen as an immutable version of {@link PlaceholderBuilder}, which can be reused and shared.<br/>
 * To create a new parser, use {@code #build(...)}.
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
    public static final Placeholder<JsonObject> JSON_OBJECT = Placeholder.buildMap(TypeMapper.of(MapReader::jsonObject), Placeholder.JSON, builder -> builder);
    public static final Placeholder<JsonArray> JSON_ARRAY = Placeholder.buildList(TypeMapper.of(ListReader::jsonArray), Placeholder.JSON, builder -> builder);
    public static final Placeholder<ResourceLocation> ID = Placeholder.build(node -> node
        .self(RegexParser.of(ResourceLocation::toString))
        .then(PatternKey.literal("namespace"), RegexParser.of(ResourceLocation::getNamespace))
        .then(PatternKey.literal("path"), RegexParser.of(ResourceLocation::getPath))
    );
    public static final Placeholder<BlockOutput> BLOCK_OUTPUT = build(BlockOutput.CODEC_COMP.codec(), builder -> builder
        .then(PatternKey.literal("id"), TypeMapper.of(BlockOutput::getId), ID)
    );
    public static final Placeholder<ItemOutput> ITEM_OUTPUT = build(
        ItemOutput.CODEC_COMP.codec(), builder -> builder
            .then(PatternKey.literal("id"), TypeMapper.of(ItemOutput::getId), ID)
            .then(PatternKey.literal("stack"), TypeMapper.of(ItemOutput::createStack), ItemStack.CODEC)
    );
    public static final Placeholder<Item> ITEM = ID.map(TypeMapper.of(Item::arch$registryName));
    public static final Placeholder<ItemStack> ITEM_STACK = ITEM_OUTPUT.map(TypeMapper.of(ItemOutput::of));
    public static final Placeholder<Block> BLOCK = ID.map(TypeMapper.of(Block::arch$registryName));
    @SuppressWarnings("unused")
    public static final Placeholder<BlockState> BLOCK_STATE = BLOCK_OUTPUT.map(TypeMapper.of(BlockOutput::of));
    public static final Placeholder<DataComponentPatch> DATA_COMPONENTS = Placeholder.build(DataComponentPatch.CODEC, PlaceholderFactory.identity());

    public static <T> Placeholder<T> build(Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return factory.apply(PlaceholderBuilder.of()).build();
    }

    public static <T> Placeholder<T> build(Codec<T> codec, PlaceholderFactory<T> factory) {
        Placeholder<T> json = JSON.map((entry, placeholder, matcher) -> Optional.ofNullable(CodecUtil.encodeJson(entry, codec).getOrThrow(PlaceholderException::new)));
        return factory.apply(PlaceholderBuilder.of()).concat(json, TypeMapper.identity()).build();
    }

    public static <T, V> Placeholder<T> buildMap(TypeMapper<T, MapReader<String, V>> mapper, Placeholder<V> valueParser, Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return factory.apply(PlaceholderBuilder.ofMap(mapper, valueParser)).build();
    }

    public static <T, E> Placeholder<T> buildList(TypeMapper<T, ListReader<E>> mapper, Placeholder<E> valueParser, Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return factory.apply(PlaceholderBuilder.ofList(mapper, valueParser)).build();
    }

    @Unmodifiable
    private final Map<PatternKey, RegexParser<T>> subNodes;

    public Placeholder(Codec<T> codec, Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        Placeholder<T> json = JSON.map((entry, placeholder, matcher) -> Optional.ofNullable(CodecUtil.encodeJson(entry, codec).getOrThrow(PlaceholderException::new)));
        this.subNodes = Collections.unmodifiableMap(factory.apply(PlaceholderBuilder.of()).concat(json, TypeMapper.identity()).getSubNodes());
    }

    public Placeholder(PlaceholderBuilder<T> builder) {
        this.subNodes = Collections.unmodifiableMap(builder.getSubNodes());
    }

    @Unmodifiable
    protected Map<PatternKey, RegexParser<T>> getSubNodes() {
        return this.subNodes;
    }

    /**
     * Create a new parser that maps the entry type using the given mapper function.
     *
     * @param mapper The function to map the entry type.
     * @param <O>    The new entry type.
     * @return A new parser that maps the entry type.
     */
    public <O> Placeholder<O> map(TypeMapper<O, T> mapper) {
        PlaceholderBuilder<T> builder = PlaceholderBuilder.of();
        builder.overwrite(this, TypeMapper.identity());
        return new Placeholder<>(builder.map(mapper));
    }

    /**
     * Analyze the placeholder and delegate to the appropriate sub-node parser.
     *
     * @param entry       The entry that provides the values.
     * @param placeholder The placeholder to be processed.
     * @param matcher     The matcher of the pattern that matched the placeholder.
     * @return The processed JsonElement, or Optional.empty() if the placeholder is not recognized.
     * @throws PlaceholderException If any error occurs during processing.
     * @apiNote This method checks if the placeholder is empty, in which case it calls the self parser.<br/>
     * If not empty, it extracts the next key segment and looks for a matching sub-node parser.<br/>
     * If a matching sub-node is found, it forwards the remaining placeholder to that parser.<br/>
     * If no matching sub-node is found, it throws a PlaceholderException.
     */
    @Override
    public Optional<JsonElement> parse(@NotNull T entry, @NotNull String placeholder, @NotNull Matcher matcher) throws PlaceholderException {
        String forwarded = RegexParser.forward(placeholder);
        String next = RegexParser.next(placeholder);
        for (Map.Entry<PatternKey, RegexParser<T>> subEntry : subNodes.entrySet()) {
            Matcher subMatcher = subEntry.getKey().pattern().matcher(next);
            if (subMatcher.find()) {
                Optional<JsonElement> result = subEntry.getValue().parse(entry, forwarded, subMatcher);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        throw new PlaceholderException("No matching key for: " + next);
    }

    public String parseStart(T entry, String placeholder, Matcher matcher) {
        try {
            JsonElement json = this.parse(entry, placeholder, matcher).orElseThrow(
                () -> new PlaceholderException("Unrecognized placeholder: " + placeholder)
            );
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                return json.getAsString();
            } else {
                return json.toString();
            }
        } catch (PlaceholderException e) {
            LOGGER.error("Error processing placeholder: " + placeholder, e);
        }
        return "${" + placeholder + "}";
    }
}
