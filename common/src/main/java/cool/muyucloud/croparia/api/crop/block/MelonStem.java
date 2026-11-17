package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

public class MelonStem extends StemBlock implements CropEntityBlock<Melon> {
    @SuppressWarnings("unchecked")
    public MelonStem(ResourceKey<? extends Block> melon, ResourceKey<? extends Block> attach, ResourceKey<? extends Item> seed, TagKey<Block> stemBase, TagKey<Block> melonBase, Properties properties) {
        super((ResourceKey<Block>) melon, (ResourceKey<Block>) attach, (ResourceKey<Item>) seed, stemBase, melonBase, properties);
    }

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random, @NotNull BlockPos pos, BlockState state) {
        this.grow(level, pos, state, random, 5);
    }

    @Override
    protected void randomTick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 9) return;
        if (Mth.nextInt(random, 0, 4) != 0) return;
        this.grow(level, pos, state, random, 1);
    }

    public void grow(Level level, BlockPos pos, BlockState state, RandomSource random, int maxGrow) {
        int i = Math.min(7, state.getValue(AGE) + Mth.nextInt(random, Math.min(2, maxGrow), Math.min(5, maxGrow)));
        if (i < 7) {
            BlockState blockState = state.setValue(AGE, i);
            level.setBlock(pos, blockState, 2);
        } else {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos melonPos = pos.relative(direction);
            BlockState melonBase = level.getBlockState(melonPos.below());
            if (level.getBlockState(melonPos).canBeReplaced() &&
                level.getBlockEntity(pos) instanceof CropEntity<?> stemEntity &&
                (melonBase.is(Blocks.FARMLAND) || melonBase.is(BlockTags.DIRT))) {
                level.setBlockAndUpdate(melonPos, CropariaBlocks.MELON_BLOCK.get().defaultBlockState());
                level.setBlockAndUpdate(pos, CropariaBlocks.MELON_ATTACH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                // Apply crop id
                if (!(level.getBlockEntity(melonPos) instanceof CropEntity<?> melonEntity)) return;
                stemEntity.getCropId().ifPresent(melonEntity::setCropId);
            }
        }
    }

    @Override
    public BlockEntityType<CropEntity<Melon>> getBlockEntityType() {
        return BlockEntities.MELON.get();
    }

    @Override
    public @NotNull Identifier getId() {
        return Objects.requireNonNull(this.arch$registryName());
    }

    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        return this.appendCropId(level, pos, stack);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        return this.getCropDrops(state, builder, () -> super.getDrops(state, builder));
    }

    @Override
    protected boolean shouldChangedStateKeepBlockEntity(@NotNull BlockState oldState) {
        return this.getBlockEntityType().isValid(oldState);
    }

    @Override
    public @NonNull MutableComponent getName() {
        return Texts.translatable(this.getDescriptionId(), Texts.translatable("melon.croparia.unknown"));
    }
}
