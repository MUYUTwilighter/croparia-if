package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.NotNull;

public class JeiInfusorRecipe extends JeiCategory<InfusorRecipe> {
    public static final JeiInfusorRecipe INSTANCE = new JeiInfusorRecipe();

    @Override
    public TypedSerializer<InfusorRecipe> getTypedSerializer() {
        return InfusorRecipe.TYPED_SERIALIZER;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull InfusorRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 72, 40).add(recipe.craftingStation());
        builder.addInputSlot(72, 8).add(recipe.getIngredient());
        builder.addInputSlot(40, 40).add(recipe.getPotion());
        builder.addOutputSlot(114, 40).add(recipe.getResult());
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull InfusorRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addDrawable(ITEM_DROP, 72, 24);
        builder.addDrawable(BLOCK_PLACE, 56, 40);
        builder.addRecipeArrow().setPosition(88, 40);
    }
}
