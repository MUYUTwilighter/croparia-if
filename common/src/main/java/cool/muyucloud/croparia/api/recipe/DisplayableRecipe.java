package cool.muyucloud.croparia.api.recipe;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.api.recipe.entry.SlotDisplay;
import cool.muyucloud.croparia.util.supplier.Mappable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.util.List;

/**
 * A generic interface for recipes that can be displayed in the recipe book, can be polymorphic to many types required
 *
 */
public interface DisplayableRecipe<I extends RecipeInput> extends Recipe<I> {
    RecipeBookCategory EMPTY_CAT = new RecipeBookCategory();
    Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("unchecked")
    default <T extends RecipeInput, R extends Recipe<T>> R adapt() {
        return (R) this;
    }

    /**
     * Get the crafting station apply to this recipe
     */
    default @NonNull SlotDisplay craftingStation() {
        List<Mappable<ItemStack>> stations = this.getTypedSerializer().getStations();
        if (stations.isEmpty()) {
            throw new IllegalStateException("No stations found!");
        }
        return ItemOutput.of(stations.getFirst().get());
    }

    TypedSerializer<? extends DisplayableRecipe<I>> getTypedSerializer();

    default @NonNull List<List<ItemStack>> getInputs() {
        return List.of();
    }

    default @NonNull List<List<ItemStack>> getOutputs() {
        return List.of();
    }

    /**
     * @return {@code RecipeType<? extends Recipe<C>>} to avoid clashing
     * @see #getTypedSerializer()
     */
    @Override
    default @NonNull RecipeType<? extends Recipe<I>> getType() {
        return (RecipeType<? extends Recipe<I>>) getTypedSerializer().getRecipeType();
    }

    /**
     * @return {@code RecipeSerializer<? extends Recipe<C>>} to avoid clashing
     * @see #getTypedSerializer()
     */
    @Override
    default @NonNull RecipeSerializer<? extends Recipe<I>> getSerializer() {
        return getTypedSerializer().getSerializer();
    }

    @Override
    default boolean isSpecial() {
        return false;
    }

    @Override
    default @NonNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default @NonNull String group() {
        return this.getTypedSerializer().getId().toString();
    }

    @Override
    default @NonNull RecipeBookCategory recipeBookCategory() {
        return EMPTY_CAT;
    }
}
