package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableIngredient;
import mezz.jei.common.gui.elements.DrawableResource;
import mezz.jei.library.ingredients.TypedIngredient;
import mezz.jei.library.render.ItemStackRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public abstract class JeiCategory<R extends DisplayableRecipe<? extends RecipeInput>> implements IRecipeCategory<R>, IRecipeType<R> {
    public abstract TypedSerializer<R> getTypedSerializer();

    @Override
    public @NotNull IRecipeType<R> getRecipeType() {
        return this;
    }

    @Override
    public @NotNull Component getTitle() {
        return Texts.translatable("gui.%s.%s.title".formatted(this.getUid().getNamespace(), this.getUid().getPath()));
    }

    @Override
    public @Nullable IDrawable getIcon() {
        List<Mappable<ItemStack>> stations = this.getTypedSerializer().getStations();
        if (stations.isEmpty()) throw new RuntimeException("Override is required if no stations are provided for " + this.getTypedSerializer().getId());
        else return toDrawable(stations.getFirst().get());
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return this.getTypedSerializer().getId();
    }

    @Override
    public @NotNull Class<? extends R> getRecipeClass() {
        return this.getTypedSerializer().getRecipeClass();
    }

    @Override
    public int getHeight() {
        return 64;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    public static final int SLOT_SIZE_HIGHLIGHT = 17;
    public static final int SLOT_SIZE_COMPAT = 16;
    public static final int SLOT_SIZE = 18;
    public static final int BUTTON_SIZE = 12;
    public static final DrawableResource ITEM_DROP = new DrawableResource(Constants.ITEM_DROP, 0, 0, 16, 16, 0, 0, 0, 0, 16, 16);
    public static final DrawableResource ELEM_INFUSE = new DrawableResource(Constants.ELEM_INFUSE, 0, 0, 16, 16, 0, 0, 0, 0, 16, 16);
    public static final DrawableResource BLOCK_PLACE = new DrawableResource(Constants.BLOCK_PLACE, 0, 0, 16, 16, 0, 0, 0, 0, 16, 16);
    public static final DrawableResource BLOCK_PLACE_UPON = new DrawableResource(Constants.BLOCK_PLACE_UPON, 0, 0, 16, 16, 0, 0, 0, 0, 16, 16);
    public static final DrawableResource LEFT_DARK = new DrawableResource(Constants.LEFT_DARK, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource RIGHT_DARK = new DrawableResource(Constants.RIGHT_DARK, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource UP_DARK = new DrawableResource(Constants.UP_DARK, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource DOWN_DARK = new DrawableResource(Constants.DOWN_DARK, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource LEFT_WHITE = new DrawableResource(Constants.LEFT_WHITE, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource RIGHT_WHITE = new DrawableResource(Constants.RIGHT_WHITE, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource UP_WHITE = new DrawableResource(Constants.UP_WHITE, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);
    public static final DrawableResource DOWN_WHITE = new DrawableResource(Constants.DOWN_WHITE, 0, 0, 12, 12, 0, 0, 0, 0, 12, 12);

    public static DrawableIngredient<ItemStack> toDrawable(ItemStack stack) {
        return new DrawableIngredient<>(Objects.requireNonNull(TypedIngredient.createAndFilterInvalid(
            Internal.getJeiRuntime().getIngredientManager(), VanillaTypes.ITEM_STACK, stack, true
        )), new ItemStackRenderer());
    }

    public static <T extends AbstractInputManager<T>> T add(IRecipeExtrasBuilder builder, T manager) {
        builder.addGuiEventListener(manager);
        builder.addDrawable(manager);
        manager.onBuild();
        return manager;
    }
}
