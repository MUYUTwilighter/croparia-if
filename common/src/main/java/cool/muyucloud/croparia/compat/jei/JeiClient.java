package cool.muyucloud.croparia.compat.jei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.jei.category.*;
import cool.muyucloud.croparia.util.supplier.Mappable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
    private static IJeiRuntime runtime = null;

    public static IJeiRuntime getRuntime() {
        if (runtime == null) {
            CropariaIf.LOGGER.warn("Usage of JEI Runtime while plugin unavailable");
            return Internal.getJeiRuntime();
        } else {
            return runtime;
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

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
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        CATEGORIES.forEach(category -> registerRecipes(registration, category));
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (JeiCategory<?> category : CATEGORIES) {
            for (Mappable<ItemStack> stack : category.getTypedSerializer().getStations()) {
                registration.addRecipeCatalysts(category.getRecipeType(), stack.get());
            }
        }
    }

    private static <R extends DisplayableRecipe<?>> void registerRecipes(@NotNull IRecipeRegistration registration, JeiCategory<R> category) {
        registration.addRecipes(category.getRecipeType(), category.getTypedSerializer().find());
    }
}
