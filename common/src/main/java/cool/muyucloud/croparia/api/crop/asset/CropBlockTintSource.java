package cool.muyucloud.croparia.api.crop.asset;

import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CropBlockTintSource implements BlockTintSource {
    @Override
    public int color(@NotNull BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(@NotNull BlockState state, BlockAndTintGetter level, @NotNull BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CropEntity<?> cropEntity)) return color(state);
        Optional<? extends AbstractCrop<?>> mayCrop = cropEntity.getCrop();
        if (mayCrop.isEmpty()) return color(state);
        AbstractCrop<?> crop = mayCrop.get();
        if (!(crop instanceof ColorAccess access)) return color(state);
        return access.getColor().getValue();
    }
}
