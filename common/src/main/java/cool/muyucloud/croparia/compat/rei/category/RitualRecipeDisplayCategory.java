package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.ReiUtil;
import cool.muyucloud.croparia.compat.rei.display.SimpleCategory;
import cool.muyucloud.croparia.compat.rei.display.SimpleDisplay;
import cool.muyucloud.croparia.util.Constants;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RitualRecipeDisplayCategory extends SimpleCategory<RitualRecipe> {
    public static final RitualRecipeDisplayCategory INSTANCE = new RitualRecipeDisplayCategory();

    @Override
    public TypedSerializer<RitualRecipe> getRecipeType() {
        return RitualRecipe.TYPED_SERIALIZER;
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> inputEntries(RecipeHolder<RitualRecipe> holder) {
        RitualRecipe recipe = holder.value();
        return Map.of(
            "block", () -> ReiUtil.toIngredient(recipe.getBlock(), stack -> stack.tooltip(Constants.BLOCK_PLACE_TOOLTIP)),
            "ingredient", () -> ReiUtil.toIngredient(recipe.getIngredient(), stack -> stack.tooltip(Constants.ITEM_DROP_TOOLTIP))
        );
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> outputEntries(RecipeHolder<RitualRecipe> holder) {
        return Map.of("result", () -> ReiUtil.toIngredient(holder.value().getResult()));
    }

    @Override
    public List<Widget> setupDisplay(SimpleDisplay<RitualRecipe> display, Rectangle bounds) {
        Widget background = Widgets.createRecipeBase(bounds);
        Widget blockArrow = Widgets.createTexturedWidget(
            Constants.BLOCK_PLACE, bounds.getCenterX() - 24, bounds.getCenterY() + 8,
            0, 0, 16, 16, 16, 16
        );
        Widget ingredientInteract = Widgets.createTexturedWidget(
            Constants.ITEM_DROP, bounds.getCenterX() - 8, bounds.getCenterY() - 8,
            0, 0, 16, 16, 16, 16
        );
        Widget resultInteract = Widgets.createArrow(new Point(bounds.getCenterX() + 8, bounds.getCenterY() + 8));
        Widget ritual = Widgets.createSlot(new Point(bounds.getCenterX() - 8, bounds.getCenterY() + 8))
            .entry(EntryStack.of(VanillaEntryTypes.ITEM, display.getRecipe().craftingStation().stack())).disableBackground().markInput().disableHighlight();
        Widget block = Widgets.createSlot(new Point(bounds.getCenterX() - 40, bounds.getCenterY() + 8))
            .entries(display.getInput("block")).markInput().disableBackground();
        Widget ingredient = Widgets.createSlot(new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 24))
            .entries(display.getInput("ingredient")).markInput().disableBackground();
        Widget result = Widgets.createSlot(new Point(bounds.getCenterX() + 32, bounds.getCenterY() + 8))
            .entries(display.getOutput("result")).markOutput().disableBackground();
        return List.of(background, blockArrow, ingredientInteract, resultInteract, ritual, block, ingredient, result);
    }
}
