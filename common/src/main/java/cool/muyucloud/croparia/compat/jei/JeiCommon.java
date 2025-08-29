package cool.muyucloud.croparia.compat.jei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.jei.category.InfusorRecipeCategory;
import cool.muyucloud.croparia.compat.jei.category.SimpleCategory;
import cool.muyucloud.croparia.util.supplier.Mappable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JeiCommon implements IModPlugin {
    public static IJeiRuntime RUNTIME;
    public static final SimpleCategory<?>[] CATEGORIES = new SimpleCategory[]{
        InfusorRecipeCategory.INSTANCE
    };

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return CropariaIf.of("jei");
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        for (SimpleCategory<?> category : CATEGORIES) {
            registration.addRecipeCategories(category);
        }
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        for (SimpleCategory<?> category : CATEGORIES) {
            registration.addRecipes(category.getRecipeType(), List.copyOf(Internal.getClientSyncedRecipes().byType(category.getTypedSerializer())));
        }
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        for (SimpleCategory<?> category : CATEGORIES) {
            for (Mappable<ItemStack> stack : category.getTypedSerializer().getStations()) {
                registration.addCraftingStation(category.getRecipeType(), stack.get());
            }
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        RUNTIME = jeiRuntime;
    }
}
