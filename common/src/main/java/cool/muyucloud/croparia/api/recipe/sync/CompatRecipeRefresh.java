package cool.muyucloud.croparia.api.recipe.sync;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.category.JeiCategory;
import dev.architectury.platform.Platform;
import net.minecraft.resources.Identifier;

import java.lang.reflect.InvocationTargetException;
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

        try {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Method getRuntime = internalClass.getMethod("getJeiRuntime");
            Object runtime;
            try {
                runtime = getRuntime.invoke(null);
            } catch (InvocationTargetException e) {
                return;
            }

            Method getRecipeManager = runtime.getClass().getMethod("getRecipeManager");
            Object recipeManager = getRecipeManager.invoke(runtime);
            Method addRecipes = recipeManager.getClass().getMethod("addRecipes", Class.forName("mezz.jei.api.recipe.types.IRecipeType"), List.class);
            Method hideRecipes = recipeManager.getClass().getMethod("hideRecipes", Class.forName("mezz.jei.api.recipe.types.IRecipeType"), Collection.class);

            for (JeiCategory<?> category : JeiClient.CATEGORIES) {
                Identifier id = category.getTypedSerializer().getId();
                if (!updatedTypes.contains(id)) {
                    continue;
                }

                List<?> previous = LAST_JEI_RECIPES.get(id);
                if (previous != null && !previous.isEmpty()) {
                    hideRecipes.invoke(recipeManager, category.getRecipeType(), previous);
                }

                List<?> current = new ArrayList<>(category.getTypedSerializer().find());
                if (!current.isEmpty()) {
                    addRecipes.invoke(recipeManager, category.getRecipeType(), current);
                }
                LAST_JEI_RECIPES.put(id, current);
            }
        } catch (ReflectiveOperationException e) {
            CropariaIf.LOGGER.debug("Failed to refresh JEI synced recipes", e);
        }
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
