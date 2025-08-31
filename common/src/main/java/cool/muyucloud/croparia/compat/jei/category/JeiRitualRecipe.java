package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.NotNull;

public class JeiRitualRecipe extends JeiCategory<RitualRecipe> {
    public static final JeiRitualRecipe INSTANCE = new JeiRitualRecipe();

    @Override
    public TypedSerializer<RitualRecipe> getTypedSerializer() {
        return RitualRecipe.TYPED_SERIALIZER;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull RitualRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 72, 40).add(recipe.craftingStation());
        builder.addInputSlot(72, 8).add(recipe.getIngredient());
        builder.addInputSlot(40, 40).add(recipe.getBlock());
        builder.addOutputSlot(112, 40).add(recipe.getResult());
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull RitualRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addDrawable(ITEM_DROP, 72, 24);
        builder.addDrawable(ELEM_INFUSE, 56, 40);
        builder.addRecipeArrow().setPosition(88, 40);
    }
}
