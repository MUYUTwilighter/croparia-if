package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.util.ProxyCategory;
import cool.muyucloud.croparia.compat.rei.util.SimpleDisplay;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class ReiCategory<R extends DisplayableRecipe<?>> implements DisplayCategory<SimpleDisplay<R>> {
    private final CategoryIdentifier<SimpleDisplay<R>> categoryIdentifier;

    public ReiCategory(ProxyCategory<R> proxy) {
        this.categoryIdentifier = proxy.getId();
    }

    @Override
    public Component getTitle() {
        return Texts.translatable("gui.%s.%s.title".formatted(this.getId().getNamespace(), this.getId().getPath()));
    }

    @Override
    public Renderer getIcon() {
        EntryIngredient[] stations = this.stations();
        if (stations.length > 0) return stations[0].getFirst();
        else throw new RuntimeException("Override is required if no stations are provided for " + this.getRecipeType().getId());
    }

    public abstract TypedSerializer<R> getRecipeType();

    public ResourceLocation getId() {
        return getCategoryIdentifier().getIdentifier();
    }

    public EntryIngredient[] stations() {
        EntryIngredient[] array = new EntryIngredient[this.getRecipeType().getStations().size()];
        int i = 0;
        for (Mappable<ItemStack> stack : this.getRecipeType().getStations()) {
            array[i] = EntryIngredients.of(stack.get());
            i++;
        }
        return array;
    }

    @Override
    public CategoryIdentifier<SimpleDisplay<R>> getCategoryIdentifier() {
        return categoryIdentifier;
    }
}
