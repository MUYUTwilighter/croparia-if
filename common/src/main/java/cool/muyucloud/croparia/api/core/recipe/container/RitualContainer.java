package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public record RitualContainer(BlockState ritual, @NotNull Collection<ItemStack> stacks,
                              @NotNull RitualStructure.Result matched) implements RecipeInput {
    public static RitualContainer of(BlockState ritual, @NotNull Collection<ItemEntity> items, @NotNull RitualStructure.Result matched) {
        return new RitualContainer(ritual, items.stream().map(ItemEntity::getItem).toList(), matched);
    }

    public static RitualContainer of(Level level, BlockPos pos, @NotNull RitualStructure.Result matched) {
        return new RitualContainer(level.getBlockState(pos), level.getEntitiesOfClass(
            ItemEntity.class, AABB.unitCubeFromLowerCorner(pos.getBottomCenter()),
            entity -> !entity.getItem().isEmpty()
        ).stream().map(ItemEntity::getItem).toList(), matched);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return i < stacks.size() ? stacks.stream().iterator().next() : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
