package cool.muyucloud.croparia.api.recipe.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.StateHolderAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.core.component.BlockProperties;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.TagUtil;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BlockInput implements SlotDisplay {
    public static final ItemStack STACK_UNKNOWN = Items.BEDROCK.getDefaultInstance();
    public static final ItemStack STACK_AIR = Items.BARRIER.getDefaultInstance();
    public static final ItemStack STACK_ANY = Items.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultInstance();
    public static final Supplier<ItemStack> STACK_PLACEHOLDER = () -> CropariaItems.PLACEHOLDER_BLOCK.get().getDefaultInstance();
    public static final BlockInput UNKNOWN = BlockInput.of(CropariaIf.of("unknown"));
    public static final BlockInput ANY = new BlockInput(null, null, BlockProperties.EMPTY, DataComponentExactPredicate.EMPTY);
    public static final MapCodec<BlockInput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("id").forGetter(BlockInput::getId),
        TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(BlockInput::getTag),
        BlockProperties.CODEC.optionalFieldOf("properties", BlockProperties.EMPTY).forGetter(BlockInput::getProperties),
        DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(BlockInput::getComponents)
    ).apply(instance, (id, tag, properties, components) -> create(id.orElse(null), tag.orElse(null), properties, components)));
    public static final Codec<BlockInput> CODEC_STR = Codec.STRING.xmap(BlockInput::create, BlockInput::getTaggable);
    public static final MultiCodec<BlockInput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.isAny()) return TestedCodec.fail(() -> "Can be encoded as empty string");
        if (toEncode.getProperties().isEmpty() && toEncode.getComponents().isEmpty())
            return TestedCodec.fail(() -> "Can be encoded as simple id or tag");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockInput> STREAM_CODEC = CodecUtil.toStream(CODEC);

    static {
        STACK_UNKNOWN.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.unknown"));
        STACK_AIR.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.air"));
        STACK_ANY.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.any"));
    }

    public static BlockInput create(String s) {
        if (s.startsWith("#")) {
            s = s.substring(1);
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.parse(s));
            return ofTag(tag);
        } else if (s.isEmpty()) {
            return ANY;
        } else {
            return of(Identifier.parse(s));
        }
    }

    protected static BlockInput create(@Nullable Identifier id, @Nullable TagKey<Block> tag, @NotNull BlockProperties properties,
                                       @NotNull DataComponentExactPredicate components) {
        BlockInput blockInput = new BlockInput(id, tag, properties, components);
        if (blockInput.isAny()) return ANY;
        else return blockInput;
    }

    public static BlockInput ofTag(Identifier id) {
        return ofTag(TagKey.create(Registries.BLOCK, id));
    }

    public static BlockInput ofTag(Identifier id, BlockProperties properties) {
        return ofTag(TagKey.create(Registries.BLOCK, id), properties);
    }

    public static BlockInput ofTag(TagKey<Block> tag) {
        return BlockInput.ofTag(tag, BlockProperties.EMPTY);
    }

    public static BlockInput ofTag(TagKey<Block> tag, BlockProperties properties) {
        return ofTag(tag, properties, DataComponentExactPredicate.EMPTY);
    }

    public static BlockInput ofTag(TagKey<Block> tag, BlockProperties properties, DataComponentExactPredicate components) {
        return new BlockInput(null, tag, properties, components);
    }

    public static BlockInput of(@NotNull Identifier id) {
        return of(id, BlockProperties.EMPTY);
    }

    public static BlockInput of(@NotNull Identifier id, BlockProperties properties) {
        return of(id, properties, DataComponentExactPredicate.EMPTY);
    }

    public static BlockInput of(@NotNull Identifier id, BlockProperties properties, DataComponentExactPredicate components) {
        return new BlockInput(id, null, properties, components);
    }

    public static BlockInput of(@NotNull Block block) {
        return of(Objects.requireNonNull(block.arch$registryName()));
    }

    public static BlockInput of(@NotNull BlockState state) {
        return of(Objects.requireNonNull(state.getBlock().arch$registryName()), BlockProperties.extract(state));
    }

    public static BlockInput of(LevelReader level, BlockPos pos) {
        return of(new BlockInWorld(level, pos, false));
    }

    public static BlockInput of(BlockInWorld block) {
        BlockState state = Objects.requireNonNull(block.getState());
        BlockEntity be = block.getEntity();
        return of(Objects.requireNonNull(state.getBlock().arch$registryName()), BlockProperties.extract(state),
            be == null ? DataComponentExactPredicate.EMPTY : DataComponentExactPredicate.allOf(be.collectComponents()));
    }

    static ItemStack getBlockItem(Block block) {
        // Attached stems have no direct BlockItem; use the seed that creates the stem.
        return block == CropariaBlocks.MELON_ATTACH.get()
            ? CropariaItems.MELON_SEED.get().getDefaultInstance()
            : block.asItem().getDefaultInstance();
    }

    @Nullable
    private final Identifier id;
    @Nullable
    private final TagKey<Block> tag;
    @NotNull
    private final BlockProperties properties;
    @NotNull
    private final DataComponentExactPredicate components;
    private transient OnLoadSupplier<ImmutableList<ItemStack>> displayStacks;
    private boolean virtualRender = false;

    protected BlockInput(@Nullable Identifier id, @Nullable TagKey<Block> tag, @NotNull BlockProperties properties, @NotNull DataComponentExactPredicate components) {
        this.id = id;
        this.tag = tag;
        if (this.id != null && this.tag != null)
            throw new IllegalArgumentException("id and tag cannot be set at the same time");
        this.properties = properties;
        this.components = components;
        this.displayStacks = OnLoadSupplier.of(() -> {
            this.virtualRender = false;
            if (this.getId().isPresent()) {
                ItemStack displayStack = BuiltInRegistries.BLOCK.getOptional(this.getId().get()).map(block -> {
                    ItemStack stack = getBlockItem(block);
                    if (stack.isEmpty()) {
                        stack = Texts.rename(STACK_PLACEHOLDER.get(), Texts.translatable(block.getDescriptionId()));
                        this.virtualRender = true;
                    }
                    stack.applyComponents(this.getComponents().asPatch());
                    stack.set(BlockProperties.TYPE, this.getProperties());
                    return stack;
                }).orElseGet(() -> {
                    DisplayableRecipe.LOGGER.error("Block with id '{}' not found, using placeholder", this.getId().get());
                    this.virtualRender = true;
                    return Texts.tooltip(STACK_PLACEHOLDER.get().copy(), Texts.literal(this.getTaggable()));
                });
                return ImmutableList.of(displayStack);
            } else if (this.getTag().isPresent()) {
                LinkedList<ItemStack> stacks = new LinkedList<>();
                for (Holder<Block> holder : TagUtil.forEntries(this.getTag().get())) {
                    Block block = holder.value();
                    ItemStack stack = getBlockItem(block);
                    if (stack.isEmpty()) {
                        stack = Texts.rename(STACK_PLACEHOLDER.get(), Texts.translatable(block.getDescriptionId()));
                        this.virtualRender = true;
                    }
                    stack.applyComponents(this.getComponents().asPatch());
                    stack.set(BlockProperties.TYPE, this.getProperties());
                    stacks.add(stack);
                }
                if (stacks.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Block tag '{}' is empty, using placeholder", this.getTag().get().location());
                    this.virtualRender = true;
                    stacks.add(Texts.tooltip(STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.copyOf(stacks);
            } else if (this.isAny()) {
                return ImmutableList.of(STACK_ANY);
            } else {
                this.virtualRender = true;
                ItemStack stack = STACK_PLACEHOLDER.get();
                stack.applyComponents(this.getComponents().asPatch());
                stack.set(BlockProperties.TYPE, this.getProperties());
                return ImmutableList.of(stack);
            }
        });
    }

    public Optional<Identifier> getId() {
        return Optional.ofNullable(id);
    }

    public Identifier getDisplayId() {
        return this.getTag().map(TagKey::location).orElse(this.getDisplayStacks().getFirst().getItem().arch$registryName());
    }

    public Optional<TagKey<Block>> getTag() {
        return Optional.ofNullable(tag);
    }

    @NotNull
    protected BlockProperties getProperties() {
        return properties;
    }

    public @NonNull DataComponentExactPredicate getComponents() {
        return components;
    }

    public void placeExample(Level level, BlockPos pos) {
        BlockOutput.create(Objects.requireNonNull(this.getExampleState().getBlock().arch$registryName()),
            this.getProperties(), this.getComponents()).setBlock(level, pos);
    }

    public BlockState getExampleState() {
        BlockState state;
        if (this.getId().isPresent()) {
            state = BuiltInRegistries.BLOCK.getValue(this.getId().get()).defaultBlockState();
        } else if (this.getTag().isPresent()) {
            Iterable<Holder<Block>> candidates = TagUtil.forEntries(this.getTag().get());
            if (candidates.iterator().hasNext()) {
                state = candidates.iterator().next().value().defaultBlockState();
            } else {
                state = Blocks.BEDROCK.defaultBlockState();
            }
        } else {
            state = CropariaBlocks.PLACEHOLDER.get().defaultBlockState();
        }
        return StateHolderAccess.apply(state, this.getProperties());
    }

    @NotNull
    public ImmutableList<ItemStack> getDisplayStacks() {
        return displayStacks.get();
    }

    public void mapStacks(Function<ImmutableList<ItemStack>, ImmutableList<ItemStack>> mapper) {
        this.displayStacks = displayStacks.map(mapper);
    }

    public String getTaggable() {
        Optional<String> id = this.getId().map(Identifier::toString);
        Optional<String> tag = this.getTag().map(t -> "#" + t.location());
        return id.orElse(tag.orElse(""));
    }

    public boolean isVirtualRender() {
        this.displayStacks.get();
        return virtualRender;
    }

    public boolean isAny() {
        return this.getId().isEmpty() && this.getTag().isEmpty() && this.getProperties().isEmpty() && this.getComponents().isEmpty();
    }

    public boolean isUnknown() {
        List<ItemStack> stacks = this.getDisplayStacks();
        if (stacks.size() != 1) return false;
        return Objects.equals(stacks.getFirst().getItem(), Items.BEDROCK) && this.isVirtualRender();
    }

    public boolean matches(@NotNull Block block) {
        if (this.getId().isPresent()) return Objects.equals(block.arch$registryName(), this.getId().get());
        else if (this.getTag().isPresent()) return TagUtil.isIn(this.getTag().get(), block);
        else return true;
    }

    public boolean matches(@NotNull BlockState state) {
        return this.matches(state, null);
    }

    public boolean matches(LevelReader level, BlockPos pos) {
        return this.matches(new BlockInWorld(level, pos, false));
    }

    public boolean matches(BlockInWorld block) {
        return this.matches(block.getState(), this.getComponents().isEmpty() ? null : block.getEntity());
    }

    public boolean matches(@Nullable BlockState state, @Nullable BlockEntity be) {
        return state != null && this.matches(state.getBlock()) && this.getProperties().isSubsetOf(state)
            && (this.getComponents().isEmpty() || be != null && this.getComponents().test(be.collectComponents()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockInput that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(tag, that.tag) && Objects.equals(properties, that.properties)
            && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tag, properties, components);
    }
}
