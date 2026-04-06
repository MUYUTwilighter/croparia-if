package cool.muyucloud.croparia.api.recipe;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.recipe.entry.SlotDisplay;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

/**
 * A generic interface for recipes that can be displayed in the recipe book, can be polymorphic to many types required
 *
 */
public interface DisplayableRecipe<C extends Container> extends Recipe<C> {
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
    default @NotNull ResourceLocation getId() {
        return this.getTypedSerializer().getId();
    }

    @Override
    default @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return this.getOutputs().stream()
            .flatMap(List::stream)
            .findFirst()
            .map(ItemStack::copy)
            .orElse(ItemStack.EMPTY);
    }

    @Override
    default boolean isSpecial() {
        return false;
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
