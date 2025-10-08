package cool.muyucloud.croparia.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ItemPlaceable {
    /**
     * Places an item at a specific position in the world.
     *
     * @param world the world to place the item in
     * @param pos   the position to place the item at
     * @param stack the item stack to place
     * @param owner the entity that owns the item (can be null)
     */
    default void placeItem(Level world, BlockPos pos, ItemStack stack, Entity owner) {
        ItemStack newStack = stack.copyAndClear();  // Creative Player is also consuming items, this is intended
        ItemEntity entity = new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.6, (double) pos.getZ() + 0.5, newStack, 0, 0, 0);
        entity.setThrower(owner);
        world.addFreshEntity(entity);
    }
}
