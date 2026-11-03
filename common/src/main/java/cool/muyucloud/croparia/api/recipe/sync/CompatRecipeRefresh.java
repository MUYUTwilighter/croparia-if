package cool.muyucloud.croparia.api.recipe.sync;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.category.JeiCategory;
import dev.architectury.platform.Platform;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

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

        IJeiRuntime runtime = JeiClient.getRuntimeOrNull();
        if (runtime == null) {
            return;
        }
        IRecipeManager recipeManager = runtime.getRecipeManager();

        for (JeiCategory<?> category : JeiClient.CATEGORIES) {
            Identifier id = category.getTypedSerializer().getId();
            if (!updatedTypes.contains(id)) {
                continue;
            }

            List<?> previous = LAST_JEI_RECIPES.get(id);
            if (previous != null && !previous.isEmpty()) {
                hideRecipes(recipeManager, category, previous);
            }

            List<?> current = new ArrayList<>(category.getTypedSerializer().find());
            if (!current.isEmpty()) {
                addRecipes(recipeManager, category, current);
            }
            LAST_JEI_RECIPES.put(id, current);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void hideRecipes(IRecipeManager recipeManager, JeiCategory<?> category, List<?> recipes) {
        recipeManager.hideRecipes((IRecipeType) category.getRecipeType(), (Collection) recipes);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addRecipes(IRecipeManager recipeManager, JeiCategory<?> category, List<?> recipes) {
        recipeManager.addRecipes((IRecipeType) category.getRecipeType(), (List) recipes);
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
