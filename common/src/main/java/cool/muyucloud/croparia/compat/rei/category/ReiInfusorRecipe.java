package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.util.ProxyCategory;
import cool.muyucloud.croparia.compat.rei.util.ReiDisplay;
import cool.muyucloud.croparia.compat.rei.util.ReiUtil;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ReiInfusorRecipe extends ReiCategory<InfusorRecipe> {
    public static final LazySupplier<EntryStack<ItemStack>> STATION = LazySupplier.of(() -> EntryStacks.of(CropariaItems.INFUSOR.get()));

    public ReiInfusorRecipe(ProxyCategory<InfusorRecipe> proxy) {
        super(proxy);
    }

    @Override
    public TypedSerializer<InfusorRecipe> getRecipeType() {
        return InfusorRecipe.TYPED_SERIALIZER;
    }

    @Override
    public List<Widget> setupDisplay(ReiDisplay<InfusorRecipe> display, Rectangle bounds) {
        Widget background = Widgets.createRecipeBase(bounds);
        Widget infusor = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() + 8)
        ).entry(STATION.get()).disableBackground().markInput().disableHighlight();
        Widget ingredient = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 24)
        ).entries(ReiUtil.toIngredient(display.getRecipe().getIngredient())).markInput().disableBackground();
        Widget element = Widgets.createSlot(
            new Point(bounds.getCenterX() - 40, bounds.getCenterY() + 8)
        ).entries(EntryIngredients.of(display.getRecipe().getPotion())).markInput().disableBackground();
        Widget result = Widgets.createSlot(
            new Point(bounds.getCenterX() + 34, bounds.getCenterY() + 8)
        ).entries(ReiUtil.toIngredient(display.getRecipe().getResult())).markOutput().disableBackground();
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
