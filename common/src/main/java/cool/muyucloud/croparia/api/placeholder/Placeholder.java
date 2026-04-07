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
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import net.minecraft.nbt.CompoundTag;
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
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder<T> implements RegexParser<T> {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static <T> Placeholder<T> lazy(Supplier<Placeholder<T>> delegate) {
        return new LazyPlaceholder<>(delegate);
    }

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
                return Optional.empty();
            }
        });
        return builder;
    });
    public static final Placeholder<JsonObject> JSON_OBJECT = Placeholder.buildMap(TypeMapper.of(MapReader::json), Placeholder.JSON, builder -> builder);
    public static final Placeholder<JsonArray> JSON_ARRAY = Placeholder.buildList(TypeMapper.of(ListReader::jsonArray), Placeholder.JSON, builder -> builder);
    public static final Placeholder<ResourceLocation> ID = Placeholder.build(node -> node
        .self(RegexParser.of(ResourceLocation::toString))
        .then(PatternKey.literal("namespace"), RegexParser.of(ResourceLocation::getNamespace))
        .then(PatternKey.literal("path"), RegexParser.of(ResourceLocation::getPath))
    );
    public static final Placeholder<CompoundTag> DATA_COMPONENTS = lazy(Builtins::dataComponents);
    public static final Placeholder<BlockOutput> BLOCK_OUTPUT = lazy(Builtins::blockOutput);
    public static final Placeholder<ItemOutput> ITEM_OUTPUT = lazy(Builtins::itemOutput);
    public static final Placeholder<Item> ITEM = lazy(Builtins::item);
    public static final Placeholder<ItemStack> ITEM_STACK = lazy(Builtins::itemStack);
    public static final Placeholder<Block> BLOCK = lazy(Builtins::block);
    @SuppressWarnings("unused")
    public static final Placeholder<BlockState> BLOCK_STATE = lazy(Builtins::blockState);

    public static <T> Placeholder<T> build(Function<PlaceholderBuilder<T>, PlaceholderBuilder<T>> factory) {
        return factory.apply(PlaceholderBuilder.of()).build();
    }

    public static <T> Placeholder<T> build(Codec<T> codec, PlaceholderFactory<T> factory) {
        Placeholder<T> json = JSON.map((entry, placeholder, matcher) -> Optional.ofNullable(CodecUtil.getOrThrow(CodecUtil.encodeJson(entry, codec), PlaceholderException::new)));
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
        Placeholder<T> json = JSON.map((entry, placeholder, matcher) -> Optional.ofNullable(CodecUtil.getOrThrow(CodecUtil.encodeJson(entry, codec), PlaceholderException::new)));
        this.subNodes = Collections.unmodifiableMap(factory.apply(PlaceholderBuilder.of()).concat(json, TypeMapper.identity()).getSubNodes());
    }

    public Placeholder(PlaceholderBuilder<T> builder) {
        this.subNodes = Collections.unmodifiableMap(builder.getSubNodes());
    }

    @Unmodifiable
    protected Map<PatternKey, RegexParser<T>> getSubNodes() {
        return this.subNodes;
    }

    public <O> Placeholder<O> map(TypeMapper<O, T> mapper) {
        PlaceholderBuilder<T> builder = PlaceholderBuilder.of();
        builder.overwrite(this, TypeMapper.identity());
        return new Placeholder<>(builder.map(mapper));
    }

    public PlaceholderBuilder<T> toBuilder() {
        PlaceholderBuilder<T> builder = PlaceholderBuilder.of();
        builder.overwrite(this, TypeMapper.identity());
        return builder;
    }

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
        throw PlaceholderException.noMatchingKey(next, forwarded, this.subNodes.keySet());
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
            LOGGER.debug("Error processing placeholder: {} (entry type: {})", placeholder, entry == null ? "null" : entry.getClass().getName(), e);
        }
        return "${" + placeholder + "}";
    }

    private static final class LazyPlaceholder<T> extends Placeholder<T> {
        private final LazySupplier<Placeholder<T>> delegate;

        private LazyPlaceholder(Supplier<Placeholder<T>> supplier) {
            super(PlaceholderBuilder.of());
            this.delegate = LazySupplier.of(supplier);
        }

        private Placeholder<T> delegate() {
            return this.delegate.get();
        }

        @Override
        public Optional<JsonElement> parse(@NotNull T entry, @NotNull String placeholder, @NotNull Matcher matcher) throws PlaceholderException {
            return this.delegate().parse(entry, placeholder, matcher);
        }

        @Override
        public String parseStart(T entry, String placeholder, Matcher matcher) {
            return this.delegate().parseStart(entry, placeholder, matcher);
        }

        @Override
        public <O> Placeholder<O> map(TypeMapper<O, T> mapper) {
            return this.delegate().map(mapper);
        }

        @Override
        public PlaceholderBuilder<T> toBuilder() {
            return this.delegate().toBuilder();
        }

        @Override
        protected Map<PatternKey, RegexParser<T>> getSubNodes() {
            return this.delegate().getSubNodes();
        }
    }

    private static final class Builtins {
        private static final LazySupplier<Placeholder<CompoundTag>> DATA_COMPONENTS_IMPL =
            LazySupplier.of(() -> Placeholder.build(CompoundTag.CODEC, PlaceholderFactory.identity()));
        private static final LazySupplier<Placeholder<BlockOutput>> BLOCK_OUTPUT_IMPL = LazySupplier.of(() -> build(BlockOutput.CODEC, builder -> builder
            .then(PatternKey.literal("id"), TypeMapper.of(BlockOutput::getId), ID)
            .thenMap(PatternKey.literal("properties"), TypeMapper.of(block -> MapReader.map(block.getProperties().getProperties())), Placeholder.STRING)
        ));
        private static final LazySupplier<Placeholder<ItemOutput>> ITEM_OUTPUT_IMPL = LazySupplier.of(() -> build(
            ItemOutput.CODEC, builder -> builder
                .then(PatternKey.literal("id"), TypeMapper.of(ItemOutput::getId), ID)
                .then(PatternKey.literal("amount"), TypeMapper.of(ItemOutput::getAmount), NUMBER)
                .then(PatternKey.literal("nbt"), TypeMapper.of(ItemOutput::getNbt), Placeholder.DATA_COMPONENTS)
                .then(PatternKey.literal("components"), TypeMapper.of(ItemOutput::getNbt), Placeholder.DATA_COMPONENTS)
                .then(PatternKey.literal("stack"), TypeMapper.of(ItemOutput::createStack), ItemStack.CODEC)
        ));
        private static final LazySupplier<Placeholder<Item>> ITEM_IMPL = LazySupplier.of(() -> ID.map(TypeMapper.of(Item::arch$registryName)));
        private static final LazySupplier<Placeholder<ItemStack>> ITEM_STACK_IMPL = LazySupplier.of(() -> ITEM_OUTPUT.map(TypeMapper.of(ItemOutput::of)));
        private static final LazySupplier<Placeholder<Block>> BLOCK_IMPL = LazySupplier.of(() -> ID.map(TypeMapper.of(Block::arch$registryName)));
        private static final LazySupplier<Placeholder<BlockState>> BLOCK_STATE_IMPL = LazySupplier.of(() -> BLOCK_OUTPUT.map(TypeMapper.of(BlockOutput::of)));

        private static Placeholder<CompoundTag> dataComponents() {
            return DATA_COMPONENTS_IMPL.get();
        }

        private static Placeholder<BlockOutput> blockOutput() {
            return BLOCK_OUTPUT_IMPL.get();
        }

        private static Placeholder<ItemOutput> itemOutput() {
            return ITEM_OUTPUT_IMPL.get();
        }

        private static Placeholder<Item> item() {
            return ITEM_IMPL.get();
        }

        private static Placeholder<ItemStack> itemStack() {
            return ITEM_STACK_IMPL.get();
        }

        private static Placeholder<Block> block() {
            return BLOCK_IMPL.get();
        }

        private static Placeholder<BlockState> blockState() {
            return BLOCK_STATE_IMPL.get();
        }
    }
}
