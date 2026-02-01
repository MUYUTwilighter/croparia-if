package cool.muyucloud.croparia.api.recipe;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.recipe.entry.SlotDisplay;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

/**
 * A generic interface for recipes that can be displayed in the recipe book, can be polymorphic to many types required
 *
 */
public interface DisplayableRecipe<C extends RecipeInput> extends Recipe<C> {
    Logger LOGGER = LogUtils.getLogger();

    /**
     * Get the crafting station apply to this recipe
     */
    @NotNull SlotDisplay craftingStation();

    TypedSerializer<? extends DisplayableRecipe<C>> getTypedSerializer();

    default @NotNull List<List<ItemStack>> getInputs() {
        return List.of();
    }

    default @NotNull List<List<ItemStack>> getOutputs() {
        return List.of();
    }

    @Override
    default @NotNull TypedSerializer<? extends DisplayableRecipe<C>> getType() {
        return getTypedSerializer();
    }

    @Override
    default @NotNull TypedSerializer<? extends DisplayableRecipe<C>> getSerializer() {
        return getTypedSerializer();
    }

    @Override
    default boolean isSpecial() {
        return false;
    }

    @SuppressWarnings("unchecked")
    default <T extends RecipeInput, R extends Recipe<T>> R adapt() {
        return (R) this;
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    default @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        List<List<ItemStack>> outputs = getOutputs();
        if (outputs.isEmpty() || outputs.getFirst().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return outputs.getFirst().getFirst();
    }
}
