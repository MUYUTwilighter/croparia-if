package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
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
            melon.getMelon().adaptKey(),
            melon.getAttach().adaptKey(),
            melon.getSeed().adaptKey(),
            Properties.ofFullCopy(Blocks.PUMPKIN_STEM).setId(ResourceKey.create(Registries.BLOCK, melon.getKey()))
        );
        this.melon = melon;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
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
                Registry<Block> registry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
                Optional<Block> optional = registry.getOptional(this.getCrop().getMelon().adaptKey());
                Optional<Block> optional2 = registry.getOptional(this.getCrop().getAttach().adaptKey());
                if (optional.isPresent() && optional2.isPresent()) {
                    level.setBlockAndUpdate(melonPos, optional.get().defaultBlockState());
                    level.setBlockAndUpdate(pos, optional2.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                }
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
