package cool.muyucloud.croparia.api.recipe.sync;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.category.JeiCategory;
import dev.architectury.platform.Platform;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeInput;

import java.lang.reflect.Method;
import java.util.*;

final class CompatRecipeRefresh {
    private static final Map<Identifier, List<?>> LAST_JEI_RECIPES = new HashMap<>();

    private CompatRecipeRefresh() {}

    static void onRecipesUpdated(Collection<Identifier> updatedTypes) {
        refreshJei(updatedTypes);
        refreshRei();
    }

    static void clear() {
        LAST_JEI_RECIPES.clear();
    }

    private static void refreshJei(Collection<Identifier> updatedTypes) {
        if (!Platform.isModLoaded("jei")) {
            return;
        }

        IJeiRuntime runtime;
        try {
            runtime = JeiClient.getRuntime();
        } catch (IllegalStateException e) {
            return;
        }
        IRecipeManager recipeManager = runtime.getRecipeManager();
        for (JeiCategory<?> category : JeiClient.CATEGORIES) {
            if (updatedTypes.contains(category.getTypedSerializer().getId())) {
                refreshCategory(category, recipeManager);
            }
        }
    }

    private static <R extends DisplayableRecipe<? extends RecipeInput>> void refreshCategory(JeiCategory<R> category, IRecipeManager recipeManager) {
        Identifier id = category.getTypedSerializer().getId();
        @SuppressWarnings("unchecked")
        List<R> previous = (List<R>) LAST_JEI_RECIPES.get(id);
        if (previous != null && !previous.isEmpty()) {
            recipeManager.hideRecipes(category.getRecipeType(), previous);
        }
        List<R> current = category.getTypedSerializer().find();
        if (!current.isEmpty()) {
            recipeManager.addRecipes(category.getRecipeType(), current);
        }
        LAST_JEI_RECIPES.put(id, current);
    }

    private static void refreshRei() {
        if (!Platform.isModLoaded("roughlyenoughitems")) {
            return;
        }

        try {
            Class<?> coreClient = Class.forName("me.shedaniel.rei.RoughlyEnoughItemsCoreClient");
            for (Method method : coreClient.getMethods()) {
                if (method.getName().equals("reloadPlugins") && method.getParameterCount() == 2) {
                    method.invoke(null, null, null);
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            CropariaIf.LOGGER.debug("Failed to refresh REI synced recipes", e);
        }
    }
}
