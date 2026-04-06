package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.rei.category.ReiCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public class ReiDisplay<R extends DisplayableRecipe<?>> implements Display {
    private final R recipe;
    private final ResourceLocation id;
    private final ReiCategory<R> category;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public ReiDisplay(R recipe, ResourceLocation id, ReiCategory<R> category) {
        this.recipe = recipe;
        this.id = id;
        this.category = category;
        this.inputs = this.recipe.getInputs().stream().map(EntryIngredients::ofItemStacks).toList();
        this.outputs = this.recipe.getOutputs().stream().map(EntryIngredients::ofItemStacks).toList();
    }

    public R getRecipe() {
        return recipe;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<ReiDisplay<R>> getCategoryIdentifier() {
        return this.category.getCategoryIdentifier();
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(getId());
    }
}
