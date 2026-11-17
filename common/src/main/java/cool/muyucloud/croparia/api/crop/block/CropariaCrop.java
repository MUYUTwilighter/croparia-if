package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.Crop;
import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CropariaCrop extends CropBlock implements CropEntityBlock<Crop> {
    public static final LazySupplier<TypeProperty> TYPE = LazySupplier.of(() -> new TypeProperty("type", CropariaIf.CONFIG.getCropTypes()));

    public CropariaCrop(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public void validateType(Level level, BlockPos pos) {
        AbstractCrop.<Crop>readCrop(level, pos).map(Crop::getType).ifPresent(type -> level.setBlock(pos, level.getBlockState(pos).setValue(TYPE.get(), TYPE.get().getName(type)), Block.UPDATE_CLIENTS));
    }

    @Override
    public void growCrops(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        super.growCrops(level, pos, state);
        this.validateType(level, pos);
    }

    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        super.randomTick(state, level, pos, random);
        this.validateType(level, pos);
    }

    /**
     * @deprecated The return value is based on {@code defaultBlockState()}, which overrides {@code TYPE}.
     * Use {@code state.setValue(this.getAgeProperty(), age)} instead。
     */
    @Override
    @Deprecated
    public @NotNull BlockState getStateForAge(int age) {
        return super.getStateForAge(age);
    }

    @Override
    public void onCropEntityReady(@NotNull Level level, @NotNull CropEntity<Crop> ce) {
        this.validateType(level, ce.getBlockPos());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TYPE.get());
    }

    @Override
    protected @NotNull ItemLike getBaseSeedId() {
        return this.asItem();
    }

    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        return this.appendCropId(level, pos, stack);
    }

    @Override
    public @NotNull Identifier getId() {
        return Objects.requireNonNull(this.arch$registryName());
    }

    @Override
    public BlockEntityType<CropEntity<Crop>> getBlockEntityType() {
        return BlockEntities.CROP.get();
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        return this.getCropDrops(state, builder, () -> super.getDrops(state, builder));
    }

    @Override
    public @NonNull MutableComponent getName() {
        return Texts.translatable(this.getDescriptionId(), Texts.translatable("crop.croparia.unknown"));
    }

    public static class TypeProperty extends Property<String> {
        private final @NotNull List<String> values;

        protected TypeProperty(String name, @NotNull List<String> values) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Crop types cannot be empty");
            }
            super(name, String.class);
            this.values = List.copyOf(values);
        }

        @Override
        public @NotNull List<String> getPossibleValues() {
            return this.values;
        }

        @Override
        public @NotNull String getName(@NotNull String value) {
            String formatted = value.trim().toLowerCase();
            if (this.getPossibleValues().contains(formatted)) return formatted;
            return this.getPossibleValues().getFirst();
        }

        @Override
        public @NotNull Optional<String> getValue(@NotNull String name) {
            String formatted = name.trim().toLowerCase();
            if (this.getPossibleValues().contains(formatted)) return Optional.of(formatted);
            return Optional.empty();
        }

        @Override
        public int getInternalIndex(@NotNull String value) {
            return this.values.indexOf(value);
        }
    }
}
