package cool.muyucloud.croparia.api.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import org.jetbrains.annotations.NotNull;

public class GreenhouseItem extends BlockItem {
    public GreenhouseItem(Block block, Item.Properties settings) {
        super(block, settings);
    }

    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        if (world.isClientSide() || stack.isEmpty()) return InteractionResult.PASS;
        pos = shouldFloat(world.getBlockState(pos).getBlock()) ? pos.above(2) : pos.above();
        world.setBlockAndUpdate(pos, this.getBlock().defaultBlockState());
        Player player = context.getPlayer();
        if (player == null || player.getAbilities().instabuild) return InteractionResult.SUCCESS;
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    public static boolean shouldFloat(Block below) {
        return !(below instanceof CropBlock || below instanceof StemBlock || below instanceof AttachedStemBlock || below instanceof SweetBerryBushBlock);
    }
}
