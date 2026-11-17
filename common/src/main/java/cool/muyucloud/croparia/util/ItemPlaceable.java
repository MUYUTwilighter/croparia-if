package cool.muyucloud.croparia.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface ItemPlaceable {
    /**
     * Places an item at a specific position in the world.
     *
     * @param world the world to place the item in
     * @param pos   the position to place the item at
     * @param stack the item stack to place
     * @param owner the entity that owns the item (can be null)
     * @apiNote The amount of the stack will be modified
     */
    default void placeItem(Level world, BlockPos pos, ItemStack stack, @Nullable Entity owner) {
        if (world.isClientSide()) return;
        ItemStack newStack;
        if (world.hasNeighborSignal(pos)) { // Place the entire stack if the block is charged by redstone
            newStack = stack.copyAndClear();
        } else {    // Otherwise, place only one item
            newStack = stack.split(1);
        }
        ItemEntity entity = CifUtil.createItemEntity(world, pos, newStack);
        if (owner != null) entity.setThrower(owner);
        world.addFreshEntity(entity);
    }
}
