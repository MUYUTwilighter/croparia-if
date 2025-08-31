package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.Constants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;

public class JeiSoakRecipe extends JeiCategory<SoakRecipe> {
    public static final JeiSoakRecipe INSTANCE = new JeiSoakRecipe();

    @Override
    public TypedSerializer<SoakRecipe> getTypedSerializer() {
        return SoakRecipe.TYPED_SERIALIZER;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull SoakRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 72, 40).add(recipe.craftingStation());
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 72, 8).add(CifUtil.addTooltip(CropariaItems.INFUSOR.get().getDefaultInstance(), Constants.SOAK_INFUSOR));
        builder.addInputSlot(40, 40).add(recipe.getInput());
        builder.addInputSlot(40, 8).add(recipe.getPotion());
        builder.addOutputSlot(114, 40).add(recipe.getOutput());
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull SoakRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(88, 40);
        builder.addDrawable(BLOCK_PLACE_UPON, 72, 24);
        builder.addDrawable(BLOCK_PLACE, 56, 40);
        builder.addDrawable(ELEM_INFUSE, 56, 8);
        builder.addRecipeArrow().setPosition(88, 40);
        builder.addText(FormattedText.of("%3.2f %%".formatted(recipe.getProbability())), 32, 16).setPosition(112, 24);
    }
}
