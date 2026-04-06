package cool.muyucloud.croparia.api.recipe.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.access.StateHolderAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.core.component.BlockProperties;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class BlockOutput implements SlotDisplay {
    public static final Codec<BlockOutput> CODEC_STR = ResourceLocation.CODEC.xmap(
        BlockOutput::create, BlockOutput::getId
    );
    public static final MapCodec<BlockOutput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(BlockOutput::getId),
        BlockProperties.CODEC.optionalFieldOf("properties").forGetter(blockOutput -> Optional.of(blockOutput.getProperties()))
    ).apply(instance, (id, properties) -> create(id, properties.orElse(BlockProperties.EMPTY))));
    public static final MultiCodec<BlockOutput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getProperties().isEmpty()) return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final ItemStack STACK_UNKNOWN = Items.BEDROCK.getDefaultInstance();
    public static final ItemStack STACK_AIR = Items.BARRIER.getDefaultInstance();

    static {
        STACK_AIR.setHoverName(Texts.translatable("tooltip.croparia.air"));
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final BlockProperties properties;
    private final transient ItemStack displayStack;

    public static BlockOutput create(@NotNull ResourceLocation id) {
        return create(id, BlockProperties.EMPTY);
    }

    protected static BlockOutput create(@NotNull ResourceLocation id, @NotNull BlockProperties properties) {
        return new BlockOutput(id, properties);
    }

    public static BlockOutput of(BlockState state) {
        return create(Objects.requireNonNull(state.getBlock().arch$registryName()), BlockProperties.extract(state));
    }

    protected BlockOutput(@NotNull ResourceLocation id, @NotNull BlockProperties properties) {
        this.id = id;
        this.properties = properties;
        this.displayStack = BuiltInRegistries.BLOCK.getOptional(this.getId()).map(block -> {
            ItemStack stack = block.asItem().getDefaultInstance();
            this.getProperties().addToTooltip(stack);
            return stack;
        }).orElseThrow(() -> new IllegalArgumentException("Unknown block: " + id));
    }

    public @NotNull ResourceLocation getId() {
        return id;
    }

    public Block getBlock() {
        return BuiltInRegistries.BLOCK.getOptional(this.getId()).orElse(null);
    }

    @NotNull
    public BlockProperties getProperties() {
        return properties;
    }

    @NotNull
    public List<ItemStack> getDisplayStacks() {
        return List.of(displayStack);
    }

    public boolean matches(@NotNull Block block) {
        return Objects.equals(block.arch$registryName(), this.getId());
    }

    public boolean matches(@NotNull BlockState state) {
        return this.matches(state.getBlock()) && this.getProperties().isSubsetOf(state);
    }

    public void setBlock(ServerLevel level, BlockPos pos) {
        BlockState state = this.getBlock().defaultBlockState();
        state = StateHolderAccess.apply(state, this.getProperties());
        level.setBlock(pos, state, 3);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockOutput that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, properties);
    }
}
