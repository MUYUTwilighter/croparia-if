package cool.muyucloud.croparia.compat.rei.category;

import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.ReiUtil;
import cool.muyucloud.croparia.compat.rei.display.SimpleCategory;
import cool.muyucloud.croparia.compat.rei.display.SimpleDisplay;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import cool.muyucloud.croparia.util.text.Texts;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SoakRecipeDisplayCategory extends SimpleCategory<SoakRecipe> {
    public static final SoakRecipeDisplayCategory INSTANCE = new SoakRecipeDisplayCategory();
    public static final LazySupplier<EntryStack<ItemStack>> STATION = LazySupplier.of(
        () -> EntryStack.of(VanillaEntryTypes.ITEM, CropariaItems.ELEMENTAL_STONE.get().getDefaultInstance())
    );

    @Override
    public TypedSerializer<SoakRecipe> getRecipeType() {
        return SoakRecipe.TYPED_SERIALIZER;
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> inputEntries(RecipeHolder<SoakRecipe> holder) {
        SoakRecipe recipe = holder.value();
        return Map.of(
            "element", () -> EntryIngredients.of(recipe.getPotion()),
            "input", () -> ReiUtil.toIngredient(recipe.getInput())
        );
    }

    @Override
    public Map<String, Supplier<EntryIngredient>> outputEntries(RecipeHolder<SoakRecipe> holder) {
        return Map.of("output", () -> ReiUtil.toIngredient(holder.value().getOutput()));
    }

    @Override
    public List<Widget> setupDisplay(SimpleDisplay<SoakRecipe> display, Rectangle bounds) {
        Widget background = Widgets.createRecipeBase(bounds);
        Widget infusor = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 24)
        ).entry(EntryStacks.of(CropariaItems.INFUSOR.get()).tooltip(
            Texts.translatable("tooltip.croparia.soak.infusor"))
        ).disableBackground().markInput().disableHighlight();
        Widget infusorArr = Widgets.createTexturedWidget(
            Constants.BLOCK_PLACE_UPON, bounds.getCenterX() - 8, bounds.getCenterY() - 8,
            0, 0, 16, 16, 16, 16
        );
        Widget input = Widgets.createSlot(
            new Point(bounds.getCenterX() - 40, bounds.getCenterY() + 8)
        ).entries(display.getInput("input")).markInput().disableBackground();
        Widget inputArr = Widgets.createTexturedWidget(
            Constants.BLOCK_PLACE, bounds.getCenterX() - 24, bounds.getCenterY() + 8,
            0, 0, 16, 16, 16, 16
        );
        Widget element = Widgets.createSlot(
            new Point(bounds.getCenterX() - 40, bounds.getCenterY() - 24)
        ).entries(display.getInput("element")).markInput().disableBackground();
        Widget elemArr = Widgets.createTexturedWidget(
            Constants.ELEM_INFUSE, bounds.getCenterX() - 24, bounds.getCenterY() - 24,
            0, 0, 16, 16, 16, 16
        );
        Widget station = Widgets.createSlot(
            new Point(bounds.getCenterX() - 8, bounds.getCenterY() + 8)
        ).entries(EntryIngredient.of(STATION.get())).markInput().disableBackground();
        Widget probability = Widgets.createLabel(
            new Point(bounds.getCenterX() + 34, bounds.getCenterY() - 8),
            Texts.literal(display.getRecipe().getProbability() * 100 + "%"));
        Widget output = Widgets.createSlot(
            new Point(bounds.getCenterX() + 34, bounds.getCenterY() + 8)
        ).entries(display.getOutput("output")).markOutput().disableBackground();
        Widget outputArr = Widgets.createArrow(new Point(bounds.getCenterX() + 8, bounds.getCenterY() + 8));
        return List.of(background, infusor, infusorArr, input, inputArr, element, elemArr, output, outputArr, probability, station);
    }
}
