package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.util.ProxyCategory;
import cool.muyucloud.croparia.compat.rei.util.ReiUtil;
import cool.muyucloud.croparia.compat.rei.util.SimpleDisplay;
import cool.muyucloud.croparia.util.Constants;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;

import java.util.List;

public class ReiRitualRecipe extends ReiCategory<RitualRecipe> {
    public ReiRitualRecipe(ProxyCategory<RitualRecipe> proxy) {
        super(proxy);
    }

    @Override
    public TypedSerializer<RitualRecipe> getRecipeType() {
        return RitualRecipe.TYPED_SERIALIZER;
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
            .entries(ReiUtil.toIngredient(display.getRecipe().getRitual())).disableBackground().markInput().disableHighlight();
        Widget block = Widgets.createSlot(new Point(bounds.getCenterX() - 40, bounds.getCenterY() + 8))
            .entries(ReiUtil.toIngredient(display.getRecipe().getBlock())).markInput().disableBackground();
        Widget ingredient = Widgets.createSlot(new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 24))
            .entries(ReiUtil.toIngredient(display.getRecipe().getIngredient())).markInput().disableBackground();
        Widget result = Widgets.createSlot(new Point(bounds.getCenterX() + 32, bounds.getCenterY() + 8))
            .entries(ReiUtil.toIngredient(display.getRecipe().getResult())).markOutput().disableBackground();
        return List.of(background, blockArrow, ingredientInteract, resultInteract, ritual, block, ingredient, result);
    }
}
