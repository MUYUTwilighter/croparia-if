package cool.muyucloud.croparia.api.core.util;

import cool.muyucloud.croparia.api.core.component.TargetPos;
import cool.muyucloud.croparia.util.CifUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a cache for item entities dropped at specific block positions to avoid redundant queries within the same game event.
 *
 */
public class DropsCache {
    private static final Map<TargetPos, Long> CACHE = new HashMap<>();

    public static boolean isQueried(Level level, BlockPos pos, long duration) {
        TargetPos targetPos = new TargetPos(level, pos);
        long lastQuery = CACHE.getOrDefault(targetPos, 0L);
        long currentTime = System.currentTimeMillis();
        return currentTime - lastQuery < duration;
    }

    /**
     * Checks if the item entities at the specified position have been queried within the current tick.
     */
    public static boolean isTickQueried(Level level, BlockPos pos) {
        int tickTime = CifUtil.toIntSafe(level.tickRateManager().millisecondsPerTick());
        TargetPos targetPos = new TargetPos(level, pos);
        long lastQuery = CACHE.getOrDefault(targetPos, 0L);
        long currentTime = System.currentTimeMillis();
        return currentTime - lastQuery < tickTime;
    }

    public static List<ItemEntity> query(Level level, BlockPos pos) {
        TargetPos key = new TargetPos(level, pos);
        CACHE.put(key, System.currentTimeMillis());
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), drop -> drop.isAlive() && !drop.getItem().isEmpty());
    }

    public static List<ItemStack> queryStacks(Level level, BlockPos pos) {
        return query(level, pos).stream().map(ItemEntity::getItem).toList();
    }

    public static void remove(Level level, BlockPos pos) {
        TargetPos key = new TargetPos(level, pos);
        CACHE.remove(key);
    }

    public static void clear() {
        CACHE.clear();
    }
}
