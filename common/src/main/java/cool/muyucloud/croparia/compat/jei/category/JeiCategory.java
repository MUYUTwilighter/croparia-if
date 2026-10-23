package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public abstract class JeiCategory<R extends DisplayableRecipe<?>> implements IRecipeCategory<R> {
    private final RecipeType<R> recipeType = RecipeType.create(
        this.getTypedSerializer().getId().getNamespace(),
        this.getTypedSerializer().getId().getPath(),
        this.getTypedSerializer().getRecipeClass()
    );

    @SuppressWarnings("unchecked")
    public <T extends DisplayableRecipe<?>> JeiCategory<T> adapt() {
        return (JeiCategory<T>) this;
    }

    public abstract TypedSerializer<R> getTypedSerializer();

    @Override
    public @NotNull RecipeType<R> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public @NotNull Component getTitle() {
        return Texts.translatable("gui.%s.%s.title".formatted(this.getUid().getNamespace(), this.getUid().getPath()));
    }

    @Override
    public @Nullable IDrawable getIcon() {
        List<Mappable<ItemStack>> stations = this.getTypedSerializer().getStations();
        if (stations.isEmpty())
            throw new RuntimeException("Override is required if no stations are provided for " + this.getTypedSerializer().getId());
        else return toDrawable(stations.get(0).get());
    }

    public @NotNull ResourceLocation getUid() {
        return this.getTypedSerializer().getId();
    }

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

    /**
     * Built lazily: JEI's runtime only exists after {@code onRuntimeAvailable}, which is later than this class' static init.
     */
    static final class Icons {
        static final IDrawableStatic ITEM_DROP = icon(Constants.ITEM_DROP);
        static final IDrawableStatic ELEM_INFUSE = icon(Constants.ELEM_INFUSE);
        static final IDrawableStatic BLOCK_PLACE = icon(Constants.BLOCK_PLACE);
        static final IDrawableStatic BLOCK_PLACE_UPON = icon(Constants.BLOCK_PLACE_UPON);
        static final IDrawableStatic LEFT_DARK = button(Constants.LEFT_DARK);
        static final IDrawableStatic RIGHT_DARK = button(Constants.RIGHT_DARK);
        static final IDrawableStatic UP_DARK = button(Constants.UP_DARK);
        static final IDrawableStatic DOWN_DARK = button(Constants.DOWN_DARK);
        static final IDrawableStatic LEFT_WHITE = button(Constants.LEFT_WHITE);
        static final IDrawableStatic RIGHT_WHITE = button(Constants.RIGHT_WHITE);
        static final IDrawableStatic UP_WHITE = button(Constants.UP_WHITE);
        static final IDrawableStatic DOWN_WHITE = button(Constants.DOWN_WHITE);

        private static IDrawableStatic icon(ResourceLocation texture) {
            return drawable(texture, 16);
        }

        private static IDrawableStatic button(ResourceLocation texture) {
            return drawable(texture, 12);
        }

        private static IDrawableStatic drawable(ResourceLocation texture, int size) {
            return JeiClient.jeiRuntime().getJeiHelpers().getGuiHelper()
                .drawableBuilder(texture, 0, 0, size, size)
                .setTextureSize(size, size)
                .build();
        }
    }

    public static IDrawable toDrawable(ItemStack stack) {
        return JeiClient.jeiRuntime().getJeiHelpers().getGuiHelper().createDrawableItemStack(stack);
    }

    public static ITypedIngredient<ItemStack> toTypedIngredient(ItemStack stack) {
        return JeiClient.jeiRuntime().getJeiHelpers().getIngredientManager().createTypedIngredient(stack).orElseThrow();
    }

    public static <T extends AbstractInputManager<T>> T add(IRecipeExtrasBuilder builder, T manager) {
        builder.addGuiEventListener(manager);
        builder.addWidget(manager);
        manager.onBuild();
        return manager;
    }
}
