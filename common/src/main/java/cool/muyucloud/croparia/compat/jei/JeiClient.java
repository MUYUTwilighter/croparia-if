package cool.muyucloud.croparia.compat.jei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.CropMaterialRecipe;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.jei.category.*;
import cool.muyucloud.croparia.compat.jei.extension.CropMaterialCategoryExtension;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.supplier.Mappable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private static @Nullable IJeiRuntime runtime;

    public static IJeiRuntime getRuntime() {
        IJeiRuntime available = runtime;
        if (available == null) {
            throw new IllegalStateException("Usage of JEI Runtime while plugin unavailable");
        }
        return available;
    }

    public static IJeiRuntime getRuntimeOrNull() {
        IJeiRuntime available = runtime;
        if (available == null) {
            CropariaIf.LOGGER.warn("Usage of JEI Runtime while plugin unavailable");
        }
        return available;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Override
    public @NotNull Identifier getPluginUid() {
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

    private static <R extends DisplayableRecipe<?>> void registerRecipes(@NotNull IRecipeRegistration registration, JeiCategory<R> category) {
        registration.addRecipes(category.getRecipeType(), category.getTypedSerializer().find());
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (JeiCategory<?> category : CATEGORIES) {
            for (Mappable<ItemStack> stack : category.getTypedSerializer().getStations()) {
                registration.addCraftingStation(category.getRecipeType(), stack.get());
            }
        }
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(CropMaterialRecipe.class, new CropMaterialCategoryExtension());
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(CropariaItems.CROP_SEED.get(), CropariaComponents.CROP_ID.get());
        registration.registerFromDataComponentTypes(CropariaItems.CROP_FRUIT.get(), CropariaComponents.CROP_ID.get());
        registration.registerFromDataComponentTypes(CropariaItems.MELON_SEED.get(), CropariaComponents.CROP_ID.get());
        registration.registerFromDataComponentTypes(CropariaItems.MELON_ITEM.get(), CropariaComponents.CROP_ID.get());
    }
}
