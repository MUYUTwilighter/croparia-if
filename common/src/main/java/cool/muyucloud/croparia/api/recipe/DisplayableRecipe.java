package cool.muyucloud.croparia.api.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

/**
 * A generic interface for recipes that can be displayed in the recipe book, can be polymorphic to many types required
 *
 */
public interface DisplayableRecipe<C extends RecipeInput> extends Recipe<C>, RecipeDisplay {
    Logger LOGGER = LogUtils.getLogger();

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
    default @NotNull TypedSerializer<? extends DisplayableRecipe<C>> recipeBookCategory() {
        return getTypedSerializer();
    }

    @Override
    default @NotNull PlacementInfo placementInfo() {
        if (this.getInputs().isEmpty()) {
            return PlacementInfo.NOT_PLACEABLE;
        }
        return PlacementInfo.create(this.getInputs().stream().map(slot -> Ingredient.of(slot.stream().map(ItemStack::getItem))).toList());
    }

    @Override
    default boolean isSpecial() {
        return false;
    }

    @Override
    default @NotNull SlotDisplay result() {
        return SlotDisplay.Empty.INSTANCE;
    }

    @Override
    default @NotNull SlotDisplay craftingStation() {
        return SlotDisplay.Empty.INSTANCE;
    }

    @Override
    default @NotNull List<RecipeDisplay> display() {
        return List.of(this);
    }

    @Override
    default @NotNull Type<? extends DisplayableRecipe<?>> type() {
        return getTypedSerializer().displayType();
    }

    @SuppressWarnings("unchecked")
    default <T extends RecipeInput, R extends Recipe<T>> R adapt() {
        return (R) this;
    }
}
