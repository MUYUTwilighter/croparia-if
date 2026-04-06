package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MelonStem extends StemBlock implements CropAccess<Melon> {
    private final Melon melon;

    public MelonStem(Melon melon) {
        super(
            melon.getMelon().get(),
            () -> melon.getSeed().get(),
            Properties.copy(Blocks.PUMPKIN_STEM)
        );
        this.melon = melon;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClientSide) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int i = Math.min(7, state.getValue(AGE) + Mth.nextInt(level.random, 2, 5));
        if (i < 7) {
            BlockState blockState = state.setValue(AGE, i);
            level.setBlock(pos, blockState, 2);
        } else {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos melonPos = pos.relative(direction);
            BlockState melonBase = level.getBlockState(melonPos.below());
            if (level.getBlockState(melonPos).canBeReplaced() && (melonBase.is(Blocks.FARMLAND) || melonBase.is(BlockTags.DIRT))) {
                level.setBlockAndUpdate(melonPos, this.getCrop().getMelon().get().defaultBlockState());
                level.setBlockAndUpdate(pos, this.getCrop().getAttach().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
            }
        }
    }

    @Override
    public Melon getCrop() {
        return this.melon;
    }

    @Override
    public @NotNull MutableComponent getName() {
        MutableComponent cropName = Texts.translatable(this.getCrop().getTranslationKey());
        return Texts.translatable("block." + CropariaIf.MOD_ID + ".melon.stem", cropName);
    }

    @Override
    public @NotNull MelonSeed asItem() {
        return this.getCrop().getSeed().get();
    }
}
