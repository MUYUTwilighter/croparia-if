package cool.muyucloud.croparia.compat.rei.category;

import com.google.common.collect.Lists;
import cool.muyucloud.croparia.api.core.recipe.CropMaterialRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.compat.rei.util.ReiDisplay;
import cool.muyucloud.croparia.compat.rei.util.ReiUtil;
import cool.muyucloud.croparia.registry.Recipes;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import net.minecraft.world.item.Items;

import java.util.List;

public class ReiCropMaterial extends ReiCategory<CropMaterialRecipe> {
    @Override
    public TypedSerializer<CropMaterialRecipe> getRecipeType() {
        return Recipes.CROP_MATERIAL;
    }

    @Override
    public List<Widget> setupDisplay(ReiDisplay<CropMaterialRecipe> display, Rectangle bounds) {
        Point startPoint = new Point(bounds.getCenterX() - 58, bounds.getCenterY() - 27);
        List<Widget> widgets = Lists.newArrayList();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 60, startPoint.y + 18)));
        widgets.add(Widgets.createResultSlotBackground(new Point(startPoint.x + 95, startPoint.y + 19)));

        CropMaterialRecipe recipe = display.getRecipe();
        List<Slot> slots = Lists.newArrayList();
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 3; x++)
                slots.add(Widgets.createSlot(new Point(startPoint.x + 1 + x * 18, startPoint.y + 1 + y * 18)).markInput());
        slots.getFirst().entries(ReiUtil.toIngredient(recipe.getCrop().getFruitItem().asItem()));
        widgets.addAll(slots);
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 95, startPoint.y + 19)).entries(
            ReiUtil.toIngredient(recipe.getOutput().map(ItemOutput::createStack).orElse(Items.AIR.getDefaultInstance()))
        ).disableBackground().markOutput());
        widgets.add(Widgets.createShapelessIcon(bounds));
        return widgets;
    }
}
