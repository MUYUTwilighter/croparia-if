package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.registry.CropariaItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JeiRitualRecipe extends JeiCategory<RitualRecipe> {
    public static final JeiRitualRecipe INSTANCE = new JeiRitualRecipe();

    @Override
    public TypedSerializer<RitualRecipe> getTypedSerializer() {
        return RitualRecipe.TYPED_SERIALIZER;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return toDrawable(CropariaItems.RITUAL_STAND.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull RitualRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 72, 40).addItemStacks(recipe.craftingStation().getDisplayStacks());
        builder.addInputSlot(72, 8).addItemStacks(recipe.getIngredient().getDisplayStacks());
        builder.addInputSlot(40, 40).addItemStacks(recipe.getBlock().getDisplayStacks());
        builder.addOutputSlot(112, 40).addItemStacks(recipe.getResult().getDisplayStacks());
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull RitualRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addDrawableWidget(Icons.ITEM_DROP).setPosition(72, 24);
        builder.addDrawableWidget(Icons.ELEM_INFUSE).setPosition(56, 40);
        builder.addRecipeArrowWidget().setPosition(88, 40);
    }
}
