package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.jetbrains.annotations.NotNull;

public class InfusorRecipeCategory extends SimpleCategory<InfusorRecipe> {
    public static final InfusorRecipeCategory INSTANCE = new InfusorRecipeCategory();

    @Override
    public TypedSerializer<InfusorRecipe> getTypedSerializer() {
        return InfusorRecipe.TYPED_SERIALIZER;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull InfusorRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addInputSlot(0, 0).add(recipe.getIngredient());
    }
}
