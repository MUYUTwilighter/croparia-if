package cool.muyucloud.croparia.compat.jei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.jei.category.*;
import cool.muyucloud.croparia.util.supplier.Mappable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
@SuppressWarnings("unused")
public class JeiClient implements IModPlugin {
    public static final List<JeiCategory<?>> CATEGORIES = List.of(
        JeiInfusorRecipe.INSTANCE,
        JeiRitualRecipe.INSTANCE,
        JeiRitualStructure.INSTANCE,
        JeiSoakRecipe.INSTANCE
    );

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return CropariaIf.of("jei");
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        for (JeiCategory<?> category : CATEGORIES) {
            registration.addRecipeCategories(category);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        for (JeiCategory<?> category : CATEGORIES) {
            registration.addRecipes((IRecipeType<Recipe<RecipeInput>>) category.getRecipeType(),
                CropariaIf.getServer() == null ? List.of() : CropariaIf.getServer().getRecipeManager().getRecipes()
                    .stream().filter(recipe -> recipe.value().getType() == category.getTypedSerializer())
                    .map(holder -> (Recipe<RecipeInput>) holder.value()).toList());
        }
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (JeiCategory<?> category : CATEGORIES) {
            for (Mappable<ItemStack> stack : category.getTypedSerializer().getStations()) {
                registration.addCraftingStation(category.getRecipeType(), stack.get());
            }
        }
    }
}
