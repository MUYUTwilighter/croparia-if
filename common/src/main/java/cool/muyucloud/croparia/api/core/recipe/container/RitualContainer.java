package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public record RitualContainer(BlockState ritual, @NotNull List<ItemStack> stacks,
                              @NotNull RitualStructure.Result matched) implements Container, Iterable<ItemStack> {
    public static RitualContainer of(BlockState ritual, @NotNull List<ItemStack> items, @NotNull RitualStructure.Result matched) {
        return new RitualContainer(ritual, items, matched);
    }

    @Override
    public boolean isEmpty() {
        return stacks.isEmpty() || matched == RitualStructure.Result.FAIL || stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int i, int count) {
        return i < stacks.size() ? stacks.get(i).split(count) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int i) {
        if (i >= stacks.size()) return ItemStack.EMPTY;
        ItemStack stack = stacks.get(i);
        stacks.set(i, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int i, @NotNull ItemStack stack) {
        if (i < stacks.size()) {
            stacks.set(i, stack);
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        stacks.replaceAll(ignored -> ItemStack.EMPTY);
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return this.stacks.iterator();
    }
}
