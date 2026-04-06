package cool.muyucloud.croparia.util;

import cool.muyucloud.croparia.util.supplier.LazySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("unused")
public class CifUtil {
    public static ResourceLocation formatId(String pattern, ResourceLocation id) {
        return ResourceLocation.tryBuild(id.getNamespace(), pattern.formatted(id.getPath()));
    }

    @SuppressWarnings("unused")
    public static boolean allNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::isNull);
    }

    public static boolean isEdible(ItemStack item) {
        return item.getItem().isEdible();
    }

    @Nullable
    public static FoodProperties getFoodProperties(ItemStack item) {
        if (isEdible(item)) {
            return item.getItem().getFoodProperties();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static ServerLevel getLevel(ResourceLocation id, MinecraftServer server) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    /**
     * Returns the block position that the player is currently looking at.
     *
     * @param player The player whose line of sight is being checked.
     * @return The block position that the player is looking at.
     */
    public static BlockPos lookingAt(@NotNull Player player) {
        Level world = player.level();
        ClipContext context = new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().multiply(5, 5, 5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult result = world.clip(context);
        return result.getBlockPos();
    }

    /**
     * Attempts to store an item stack in a container below the specified position.
     *
     * @param world The world where the item will be stored.
     * @param pos   The position below which the container is located.
     * @param stack The item stack to be stored.
     * @return The remaining item stack if it couldn't be fully stored, or an empty stack if it was fully stored.
     */
    public static ItemStack transferItemNear(Level world, BlockPos pos, ItemStack stack) {
        for (Direction d : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(d.getNormal()));
            if (neighbor instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    if (!container.canPlaceItem(i, stack)) {
                        continue;
                    }
                    ItemStack stored = container.getItem(i);
                    if (ItemStack.isSameItemSameTags(stored, stack) || stored.isEmpty()) {
                        int capacity = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
                        int room = capacity - stored.getCount();
                        if (room == 0) continue;
                        int toMove = Math.min(room, stack.getCount());
                        ItemStack moved = stack.split(toMove);
                        moved.setCount(stored.getCount() + moved.getCount());
                        container.setItem(i, moved);
                        container.setChanged();
                    }
                }
            }
        }
        return stack;
    }

    /**
     * Exports an item to the world, attempting to store it in a container below the specified position.
     * If the item cannot be stored, it will be added to the player's inventory or dropped as an item entity.
     *
     * @param world  the level to export the item to
     * @param pos    the position to export the item at
     * @param stack  the item stack to export
     * @param player the player to add the item to, or null to drop the item
     */
    public static void exportItem(Level world, BlockPos pos, ItemStack stack, @Nullable Player player) {
        ItemStack remain = transferItemNear(world, pos, stack);
        if (remain.isEmpty()) {
            return;
        }
        if (player != null) {
            player.addItem(remain);
        }
        if (remain.isEmpty()) {
            return;
        }
        world.addFreshEntity(createItemEntity(world, pos, remain));
    }

    public static ItemEntity createItemEntity(Level world, BlockPos pos, ItemStack stack) {
        return new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.6, (double) pos.getZ() + 0.5, stack, 0, 0, 0);
    }

    public static int toIntSafe(long value) {
        return toIntSafe((double) value);
    }

    public static int toIntSafe(float value) {
        return toIntSafe((double) value);
    }

    public static int toIntSafe(double value) {
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T castUnsafe(Object o) {
        return (T) o;
    }

    public static <T> LazySupplier<Field> forField(Class<T> clz, String name) {
        return LazySupplier.of(() -> {
            try {
                Field field = clz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> LazySupplier<Constructor<T>> forConstructor(Class<T> clz, Class<?>... paramTypes) {
        return LazySupplier.of(() -> {
            try {
                Constructor<T> constructor = clz.getConstructor(paramTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> LazySupplier<Constructor<T>> forConstructor(String name, Class<?>... paramTypes) {
        try {
            return forConstructor((Class<T>) Class.forName(name), paramTypes);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
