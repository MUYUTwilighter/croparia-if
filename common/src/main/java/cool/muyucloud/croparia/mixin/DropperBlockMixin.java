package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.util.ItemPlaceable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {
    @Inject(
        method = "dispenseFrom", cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onDrop(ServerLevel world, BlockPos pos, CallbackInfo ci, net.minecraft.core.BlockSource blockSource, net.minecraft.world.level.block.entity.DispenserBlockEntity dispenserBlockEntity, int slot, ItemStack itemStack) {
        BlockState blockState = world.getBlockState(pos);
        pos = pos.offset(blockState.getValue(DropperBlock.FACING).getNormal());
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof ItemPlaceable placeable) {
            placeable.placeItem(world, pos, itemStack.split(1), null);
            ci.cancel();
        }
    }
}
