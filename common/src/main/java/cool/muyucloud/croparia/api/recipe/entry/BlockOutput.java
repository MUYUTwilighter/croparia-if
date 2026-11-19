package cool.muyucloud.croparia.api.recipe.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.access.StateHolderAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.core.component.BlockProperties;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BlockOutput implements SlotDisplay {
    public static final Codec<BlockOutput> CODEC_STR = Identifier.CODEC.xmap(
        BlockOutput::create, BlockOutput::getId
    );
    public static final MapCodec<BlockOutput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(BlockOutput::getId),
        BlockProperties.CODEC.optionalFieldOf("properties", BlockProperties.EMPTY).forGetter(BlockOutput::getProperties),
        DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(BlockOutput::getComponents)
    ).apply(instance, BlockOutput::create));
    public static final MultiCodec<BlockOutput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getProperties().isEmpty() && toEncode.getComponents().isEmpty()) return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockOutput> STREAM_CODEC = CodecUtil.toStream(CODEC);
    public static final Supplier<ItemStack> STACK_UNKNOWN = Items.BEDROCK::getDefaultInstance;
    public static final Supplier<ItemStack> STACK_AIR = BlockInput.STACK_AIR;

    @NotNull
    private final Identifier id;
    @NotNull
    private final BlockProperties properties;
    @NotNull
    private final DataComponentExactPredicate components;
    private final transient OnLoadSupplier<ItemStack> displayStack;

    public static BlockOutput create(@NotNull Identifier id) {
        return create(id, BlockProperties.EMPTY);
    }

    public static BlockOutput create(@NotNull Identifier id, @NotNull BlockProperties properties) {
        return create(id, properties, DataComponentExactPredicate.EMPTY);
    }

    public static BlockOutput create(@NotNull Identifier id, @NotNull BlockProperties properties, @NotNull DataComponentExactPredicate components) {
        return new BlockOutput(id, properties, components);
    }

    public static BlockOutput of(BlockState state) {
        return create(Objects.requireNonNull(state.getBlock().arch$registryName()), BlockProperties.extract(state));
    }

    public static BlockOutput of(LevelReader level, BlockPos pos) {
        return of(new BlockInWorld(level, pos, false));
    }

    public static BlockOutput of(BlockInWorld block) {
        BlockState state = Objects.requireNonNull(block.getState());
        BlockEntity be = block.getEntity();
        return create(Objects.requireNonNull(state.getBlock().arch$registryName()), BlockProperties.extract(state),
            be == null ? DataComponentExactPredicate.EMPTY : DataComponentExactPredicate.allOf(be.collectComponents()));
    }

    protected BlockOutput(@NotNull Identifier id, @NotNull BlockProperties properties, @NotNull DataComponentExactPredicate components) {
        this.id = id;
        this.properties = properties;
        this.components = components;
        Block block = BuiltInRegistries.BLOCK.getOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown block: " + id));
        if (!components.isEmpty() && !block.defaultBlockState().hasBlockEntity()) {
            throw new IllegalArgumentException("Block cannot store components without a block entity: " + id);
        }
        this.displayStack = OnLoadSupplier.of(() -> {
            ItemStack stack = BlockInput.getBlockItem(block);
            if (stack.isEmpty()) {
                stack = block.defaultBlockState().isAir() ? STACK_AIR.get()
                    : Texts.rename(BlockInput.STACK_PLACEHOLDER.get(), Texts.translatable(block.getDescriptionId()));
            }
            stack.applyComponents(this.getComponents().asPatch());
            stack.set(BlockProperties.TYPE, this.getProperties());
            return stack;
        });
    }

    public @NotNull Identifier getId() {
        return id;
    }

    public Block getBlock() {
        return BuiltInRegistries.BLOCK.getOptional(this.getId()).orElse(null);
    }

    @NotNull
    public BlockProperties getProperties() {
        return properties;
    }

    public @NotNull DataComponentExactPredicate getComponents() {
        return components;
    }

    @NotNull
    public List<ItemStack> getDisplayStacks() {
        return List.of(this.displayStack.get());
    }

    public boolean matches(@NotNull Block block) {
        return Objects.equals(block.arch$registryName(), this.getId());
    }

    public boolean matches(@NotNull BlockState state) {
        return this.getComponents().isEmpty() && this.matches(state.getBlock()) && this.getProperties().isSubsetOf(state);
    }

    public boolean matches(LevelReader level, BlockPos pos) {
        return this.matches(new BlockInWorld(level, pos, false));
    }

    // BlockInWorld can return null for an unloaded chunk when loadChunks is false.
    @SuppressWarnings("ConstantValue")
    public boolean matches(BlockInWorld block) {
        BlockState state = block.getState();
        if (state == null || !this.matches(state.getBlock()) || !this.getProperties().isSubsetOf(state)) return false;
        if (this.getComponents().isEmpty()) return true;
        BlockEntity be = block.getEntity();
        return be != null && this.getComponents().test(be.collectComponents());
    }

    public void setBlock(Level level, BlockPos pos) {
        BlockState state = StateHolderAccess.apply(this.getBlock().defaultBlockState(), this.getProperties());
        if (!level.setBlock(pos, state, Block.UPDATE_ALL) && !level.getBlockState(pos).equals(state)) return;
        if (!this.getComponents().isEmpty()) {
            BlockEntity be = Objects.requireNonNull(level.getBlockEntity(pos), "Missing block entity for " + this.getId());
            // applyComponents retains only values in the patch: include existing components as well.
            DataComponentExactPredicate merged = DataComponentExactPredicate.allOf(
                PatchedDataComponentMap.fromPatch(be.collectComponents(), this.getComponents().asPatch()));
            be.applyComponents(DataComponentMap.EMPTY, merged.asPatch());
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockOutput that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(properties, that.properties) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, properties, components);
    }
}
