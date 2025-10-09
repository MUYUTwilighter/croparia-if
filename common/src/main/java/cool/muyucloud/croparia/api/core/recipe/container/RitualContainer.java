package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public record RitualContainer(BlockState ritual, @NotNull List<ItemStack> stacks,
                              @NotNull RitualStructure.Result matched) implements RecipeInput, Iterable<ItemStack> {
    public static RitualContainer of(BlockState ritual, @NotNull List<ItemStack> items, @NotNull RitualStructure.Result matched) {
        return new RitualContainer(ritual, items, matched);
    }

    @Override
    public boolean isEmpty() {
        return stacks.isEmpty() || matched == RitualStructure.Result.FAIL || stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return this.stacks.iterator();
    }
}
