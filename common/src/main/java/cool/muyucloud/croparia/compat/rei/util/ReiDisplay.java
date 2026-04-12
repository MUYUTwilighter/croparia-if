package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.rei.category.ReiCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public class ReiDisplay<R extends DisplayableRecipe<?>> implements Display {
    private final R recipe;
    private final ResourceKey<Recipe<?>> id;
    private final ReiCategory<R> category;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public ReiDisplay(RecipeHolder<? extends R> holder, ReiCategory<R> category) {
        this.recipe = holder.value();
        this.id = holder.id();
        this.category = category;
        this.inputs = this.recipe.getInputs().stream().map(EntryIngredients::ofItemStacks).toList();
        this.outputs = this.recipe.getOutputs().stream().map(EntryIngredients::ofItemStacks).toList();
    }

    public ReiDisplay(R recipe, Identifier id, ReiCategory<R> category) {
        this(new RecipeHolder<>(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), recipe), category);
    }

    public R getRecipe() {
        return recipe;
    }

    public ResourceKey<Recipe<?>> getId() {
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
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(getId().identifier());
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
