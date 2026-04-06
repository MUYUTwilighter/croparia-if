package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.element.Element;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class InfusorContainer implements Container, Iterable<ItemStack> {
    @NotNull
    private final Element element;
    @NotNull
    private final List<ItemStack> items;

    public static InfusorContainer of(Element element, List<ItemStack> items) {
        return new InfusorContainer(element, items);
    }

    public InfusorContainer(@NotNull Element element, @NotNull List<ItemStack> items) {
        this.element = element;
        this.items = items;
    }

    public @NotNull Element getElement() {
        return element;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return i >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(i);
    }

    @Override
    public @NotNull ItemStack removeItem(int i, int count) {
        if (i >= this.getContainerSize()) return ItemStack.EMPTY;
        return this.items.get(i).split(count);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int i) {
        if (i >= this.getContainerSize()) return ItemStack.EMPTY;
        ItemStack stack = this.items.get(i);
        this.items.set(i, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean isEmpty() {
        return this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void setItem(int i, @NotNull ItemStack stack) {
        if (i < this.getContainerSize()) {
            this.items.set(i, stack);
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
        this.items.replaceAll(ignored -> ItemStack.EMPTY);
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return this.items.iterator();
    }
}
