package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.ReiUtil;
import cool.muyucloud.croparia.compat.rei.display.SimpleCategory;
import cool.muyucloud.croparia.compat.rei.display.SimpleDisplay;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InfusorRecipeDisplayCategory extends SimpleCategory<InfusorRecipe> {
    public static final InfusorRecipeDisplayCategory INSTANCE = new InfusorRecipeDisplayCategory();
    public static final LazySupplier<EntryStack<ItemStack>> STATION = LazySupplier.of(() -> EntryStacks.of(CropariaItems.INFUSOR.get()));

    @Override
    public TypedSerializer<InfusorRecipe> getRecipeType() {
        return InfusorRecipe.TYPED_SERIALIZER;
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> inputEntries(RecipeHolder<InfusorRecipe> holder) {
        InfusorRecipe recipe = holder.value();
        return Map.of(
            "element", () -> EntryIngredients.of(recipe.getPotion()),
            "ingredient", () -> ReiUtil.toIngredient(recipe.getIngredient(), stack -> stack.tooltip(Constants.ITEM_DROP_TOOLTIP))
        );
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> outputEntries(RecipeHolder<InfusorRecipe> holder) {
        return Map.of("result", () -> ReiUtil.toIngredient(holder.value().getResult()));
    }

    @Override
    public List<Widget> setupDisplay(SimpleDisplay<InfusorRecipe> display, Rectangle bounds) {
        Widget background = Widgets.createRecipeBase(bounds);
        Widget infusor = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() + 8)
        ).entry(STATION.get()).disableBackground().markInput().disableHighlight();
        Widget ingredient = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 24)
        ).entries(display.getInput("ingredient")).markInput().disableBackground();
        Widget element = Widgets.createSlot(
            new Point(bounds.getCenterX() - 40, bounds.getCenterY() + 8)
        ).entries(display.getInput("element")).markInput().disableBackground();
        Widget result = Widgets.createSlot(
            new Point(bounds.getCenterX() + 34, bounds.getCenterY() + 8)
        ).entries(display.getOutput("result")).markOutput().disableBackground();
        Widget itemDrop = Widgets.createTexturedWidget(
            Constants.ITEM_DROP, bounds.getCenterX() - 8, bounds.getCenterY() - 8,
            0, 0, 16, 16, 16, 16
        );
        Widget elemInfuse = Widgets.createTexturedWidget(
            Constants.ELEM_INFUSE, bounds.getCenterX() - 24, bounds.getCenterY() + 8,
            0, 0, 16, 16, 16, 16
        );
        Widget arrow = Widgets.createArrow(new Point(bounds.getCenterX() + 8, bounds.getCenterY() + 8));
        return List.of(background, infusor, ingredient, element, result, itemDrop, elemInfuse, arrow);
    }
}
