package cool.muyucloud.croparia.compat.jei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.jei.category.InfusorRecipeCategory;
import cool.muyucloud.croparia.compat.jei.category.SimpleCategory;
import cool.muyucloud.croparia.util.supplier.Mappable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
@SuppressWarnings("unused")
public class JeiCommon implements IModPlugin {
    public static IJeiRuntime RUNTIME;
    public static final List<SimpleCategory<?>> CATEGORIES = List.of(
        InfusorRecipeCategory.INSTANCE
    );

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
    @SuppressWarnings("unchecked")
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        for (SimpleCategory<?> category : CATEGORIES) {
            registration.addRecipes(
                (IRecipeType<Recipe<RecipeInput>>) category.getRecipeType(),
                Internal.getClientSyncedRecipes().byType(
                    (RecipeType<Recipe<RecipeInput>>) category.getTypedSerializer()
                ).stream().map(RecipeHolder::value).toList()
            );
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
