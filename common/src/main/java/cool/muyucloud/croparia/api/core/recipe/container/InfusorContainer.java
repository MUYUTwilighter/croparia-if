package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.element.Element;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class InfusorContainer implements RecipeInput, Iterable<ItemStack> {
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
    public @NotNull ItemStack getItem(int i) {
        return i >= this.size() ? ItemStack.EMPTY : this.items.get(i);
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return this.items.iterator();
    }
}
