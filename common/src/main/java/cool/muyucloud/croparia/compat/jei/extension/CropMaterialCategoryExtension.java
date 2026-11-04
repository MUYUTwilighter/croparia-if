package cool.muyucloud.croparia.compat.jei.extension;

import cool.muyucloud.croparia.api.core.recipe.CropMaterialRecipe;
import cool.muyucloud.croparia.api.recipe.entry.ItemInput;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CropMaterialCategoryExtension implements ICraftingCategoryExtension<CropMaterialRecipe> {
    @Override
    public @NonNull List<SlotDisplay> getIngredients(@NonNull RecipeHolder<CropMaterialRecipe> holder) {
        return List.of((SlotDisplay) ItemInput.of(holder.value().getCrop().getFruitItem().getDefaultInstance()));
    }
}
