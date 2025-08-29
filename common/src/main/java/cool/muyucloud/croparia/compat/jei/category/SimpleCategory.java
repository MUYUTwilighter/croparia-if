package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.util.text.Texts;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableIngredient;
import mezz.jei.library.ingredients.TypedIngredient;
import mezz.jei.library.render.ItemStackRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class SimpleCategory<R extends DisplayableRecipe<? extends RecipeInput>> implements IRecipeCategory<R>, IRecipeType<R> {
    public abstract TypedSerializer<R> getTypedSerializer();

    @Override
    public @NotNull IRecipeType<R> getRecipeType() {
        return this;
    }

    @Override
    public @NotNull Component getTitle() {
        return Texts.translatable("gui.%s.%s".formatted(this.getUid().getNamespace(), this.getUid().getPath()));
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return toDrawable(this.getTypedSerializer().getStations().getFirst().get());
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return this.getTypedSerializer().getId().orElseThrow();
    }

    @Override
    public @NotNull Class<? extends R> getRecipeClass() {
        return this.getTypedSerializer().getRecipeClass();
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public int getWidth() {
        return 128;
    }

    public static DrawableIngredient<ItemStack> toDrawable(ItemStack stack) {
        return new DrawableIngredient<>(Objects.requireNonNull(TypedIngredient.createAndFilterInvalid(
            Internal.getJeiRuntime().getIngredientManager(), VanillaTypes.ITEM_STACK, stack, true
        )), new ItemStackRenderer());
    }
}
