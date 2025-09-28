package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableList;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractCrop implements TranslatableEntry {
    public static final Placeholder<AbstractCrop> PLACEHOLDER = Placeholder.build(node -> node
        .then(Pattern.compile("material"), AbstractCrop::getMaterial, Material.PLACEHOLDER)
        .then(Pattern.compile("result"), AbstractCrop::getResult, Placeholder.ITEM_STACK)
        .concat(TranslatableEntry.PLACEHOLDER, crop -> crop));

    protected transient OnLoadSupplier<List<ItemStack>> results = OnLoadSupplier.of(() -> {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : this.getMaterial().getItems()) {
            items.add(item.getDefaultInstance().copyWithCount(Math.min(item.getDefaultMaxStackSize(), this.getMaterial().getCount())));
        }
        if (items.isEmpty()) items.add(ItemStack.EMPTY);
        return ImmutableList.copyOf(items);
    });

    public ItemStack getResult() {
        return this.getResults().getFirst();
    }

    public List<ItemStack> getResults() {
        return this.results.get();
    }

    public abstract @NotNull Material getMaterial();

    public ItemStack getMaterialStack() {
        return this.getMaterial().getStack();
    }

    public String getMaterialName() {
        return this.getMaterial().getName();
    }

    public abstract void onRegister();

    @Override
    public Placeholder<? extends AbstractCrop> placeholder() {
        return PLACEHOLDER;
    }
}
