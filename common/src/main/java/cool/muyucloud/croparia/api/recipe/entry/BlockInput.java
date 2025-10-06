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
import net.jcip.annotations.Immutable;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Immutable
@SuppressWarnings("unused")
public class BlockInput implements SlotDisplay {
    public static final ItemStack STACK_UNKNOWN = Items.BEDROCK.getDefaultInstance();
    public static final ItemStack STACK_AIR = Items.BARRIER.getDefaultInstance();
    public static final ItemStack STACK_ANY = Items.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultInstance();
    public static final Supplier<ItemStack> STACK_PLACEHOLDER = () -> CropariaItems.PLACEHOLDER_BLOCK.get().getDefaultInstance();
    public static final BlockInput UNKNOWN = BlockInput.of(CropariaIf.of("unknown"));
    public static final BlockInput ANY = new BlockInput(null, null, BlockProperties.EMPTY);
    public static final MapCodec<BlockInput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(ResourceLocation.CODEC.optionalFieldOf("id").forGetter(BlockInput::getId), TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(BlockInput::getTag), BlockProperties.CODEC.optionalFieldOf("properties", BlockProperties.EMPTY).forGetter(BlockInput::getProperties)).apply(instance, (id, tag, properties) -> create(id.orElse(null), tag.orElse(null), properties)));
    public static final Codec<BlockInput> CODEC_STR = Codec.STRING.xmap(BlockInput::create, BlockInput::getTaggable);
    public static final MultiCodec<BlockInput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.isAny()) return TestedCodec.fail(() -> "Can be encoded as empty string");
        if (toEncode.getProperties().equals(BlockProperties.EMPTY))
            return TestedCodec.fail(() -> "Can be encoded as simple id or tag");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockInput> STREAM_CODEC = CodecUtil.toStream(CODEC);
    public static final Type<BlockInput> TYPE = new Type<>(CODEC_COMP, STREAM_CODEC);

    static {
        STACK_UNKNOWN.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.unknown"));
        STACK_AIR.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.air"));
        STACK_ANY.set(DataComponents.CUSTOM_NAME, Texts.translatable("tooltip.croparia.any"));
    }

    public static BlockInput create(String s) {
        if (s.startsWith("#")) {
            s = s.substring(1);
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(s));
            return ofTag(tag);
        } else if (s.isEmpty()) {
            return ANY;
        } else {
            return of(ResourceLocation.parse(s));
        }
    }

    protected static BlockInput create(@Nullable ResourceLocation id, @Nullable TagKey<Block> tag, @NotNull BlockProperties properties) {
        BlockInput blockInput = new BlockInput(id, tag, properties);
        if (blockInput.isAny()) return ANY;
        else return blockInput;
    }

    public static BlockInput ofTag(ResourceLocation id) {
        return ofTag(TagKey.create(Registries.BLOCK, id));
    }

    public static BlockInput ofTag(ResourceLocation id, BlockProperties properties) {
        return ofTag(TagKey.create(Registries.BLOCK, id), properties);
    }

    public static BlockInput ofTag(TagKey<Block> tag) {
        return BlockInput.ofTag(tag, BlockProperties.EMPTY);
    }

    public static BlockInput ofTag(TagKey<Block> tag, BlockProperties properties) {
        return new BlockInput(null, tag, properties);
    }

    public static BlockInput of(@NotNull ResourceLocation id) {
        return of(id, BlockProperties.EMPTY);
    }

    public static BlockInput of(@NotNull ResourceLocation id, BlockProperties properties) {
        return new BlockInput(id, null, properties);
    }

    public static BlockInput of(@NotNull Block block) {
        return of(Objects.requireNonNull(block.arch$registryName()));
    }

    public static BlockInput of(@NotNull BlockState state) {
        return new BlockInput(Objects.requireNonNull(state.getBlock().arch$registryName()), null, BlockProperties.extract(state));
    }

    @Nullable
    private final ResourceLocation id;
    @Nullable
    private final TagKey<Block> tag;
    @NotNull
    private final BlockProperties properties;
    private transient OnLoadSupplier<ImmutableList<ItemStack>> displayStacks;
    private boolean virtualRender = false;

    protected BlockInput(@Nullable ResourceLocation id, @Nullable TagKey<Block> tag, @NotNull BlockProperties properties) {
        this.id = id;
        this.tag = tag;
        if (this.id != null && this.tag != null)
            throw new IllegalArgumentException("id and tag cannot be set at the same time");
        this.properties = properties;
        this.displayStacks = OnLoadSupplier.of(() -> {
            if (this.getId().isPresent()) {
                ItemStack displayStack = BuiltInRegistries.BLOCK.getOptional(this.getId().get()).map(block -> {
                    ItemStack stack = block.asItem().getDefaultInstance();
                    if (stack.isEmpty()) {
                        stack = Texts.rename(STACK_PLACEHOLDER.get(), Texts.translatable(block.getDescriptionId()));
                        this.virtualRender = true;
                    }
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
                    ItemStack stack = block.asItem().getDefaultInstance();
                    if (stack.isEmpty()) {
                        stack = Texts.rename(STACK_PLACEHOLDER.get(), Texts.translatable(block.getDescriptionId()));
                        this.virtualRender = true;
                    }
                    stack.set(BlockProperties.TYPE, this.getProperties());
                    stacks.add(stack);
                }
                if (stacks.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Block tag '{}' is empty, using placeholder", this.getTag().get().location());
                    this.virtualRender = true;
                    stacks.add(Texts.tooltip(STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.copyOf(stacks);
            } else if (this.getProperties().isEmpty()) {
                return ImmutableList.of(STACK_ANY);
            } else {
                this.virtualRender = true;
                ItemStack stack = STACK_PLACEHOLDER.get();
                stack.set(BlockProperties.TYPE, this.getProperties());
                return ImmutableList.of(stack);
            }
        });
    }

    public Optional<ResourceLocation> getId() {
        return Optional.ofNullable(id);
    }

    public ResourceLocation getDisplayId() {
        return this.getTag().map(TagKey::location).orElse(this.getDisplayStacks().getFirst().getItem().arch$registryName());
    }

    public Optional<TagKey<Block>> getTag() {
        return Optional.ofNullable(tag);
    }

    @NotNull
    protected BlockProperties getProperties() {
        return properties;
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
        Optional<String> id = this.getId().map(ResourceLocation::toString);
        Optional<String> tag = this.getTag().map(t -> "#" + t.location());
        return id.orElse(tag.orElse(""));
    }

    public boolean isVirtualRender() {
        return virtualRender;
    }

    public boolean isAny() {
        return this.getId().isEmpty() && this.getTag().isEmpty() && this.getProperties().isEmpty();
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
        return this.matches(state.getBlock()) && this.getProperties().isSubsetOf(state);
    }

    @Override
    @NotNull
    public <T> Stream<T> resolve(ContextMap contextMap, DisplayContentsFactory<T> factory) {
        if (factory instanceof DisplayContentsFactory.ForStacks<T> forStacks) {
            return this.getDisplayStacks().stream().map(forStacks::forStack);
        }
        return Stream.empty();
    }

    @Override
    @NotNull
    public Type<? extends SlotDisplay> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockInput that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(tag, that.tag) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tag, properties);
    }
}
