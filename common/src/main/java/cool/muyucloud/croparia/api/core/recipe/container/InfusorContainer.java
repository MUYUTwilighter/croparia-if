package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.element.Element;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;

public class InfusorContainer implements RecipeInput, Iterable<ItemStack> {
    @NonNull
    private final Element element;
    @NonNull
    private final List<ItemStack> items;

    public static InfusorContainer of(Element element, List<ItemStack> items) {
        return new InfusorContainer(element, items);
    }

    public InfusorContainer(@NonNull Element element, @NonNull List<ItemStack> items) {
        this.element = element;
        this.items = items;
    }

    public @NonNull Element getElement() {
        return element;
    }

    @Override
    public @NonNull ItemStack getItem(int i) {
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
    public @NonNull Iterator<ItemStack> iterator() {
        return this.items.iterator();
    }
}
