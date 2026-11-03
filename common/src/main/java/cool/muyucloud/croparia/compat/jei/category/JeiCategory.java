package cool.muyucloud.croparia.compat.jei.category;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public abstract class JeiCategory<R extends DisplayableRecipe<? extends RecipeInput>> implements IRecipeCategory<R> {
    private final IRecipeType<R> recipeType = IRecipeType.create(
        this.getTypedSerializer().getId().getNamespace(),
        this.getTypedSerializer().getId().getPath(),
        this.getTypedSerializer().getRecipeClass()
    );

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends DisplayableRecipe<I>> JeiCategory<T> adapt() {
        return (JeiCategory<T>) this;
    }

    public abstract TypedSerializer<R> getTypedSerializer();

    @Override
    public @NonNull IRecipeType<R> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public @NonNull Component getTitle() {
        return Texts.translatable("gui.%s.%s.title".formatted(this.getUid().getNamespace(), this.getUid().getPath()));
    }

    @Override
    public @Nullable IDrawable getIcon() {
        List<Mappable<ItemStack>> stations = this.getTypedSerializer().getStations();
        if (stations.isEmpty()) throw new RuntimeException("Override is required if no stations are provided for " + this.getTypedSerializer().getId());
        else return toDrawable(stations.getFirst().get());
    }

    public @NonNull Identifier getUid() {
        return this.getTypedSerializer().getId();
    }

    public @NonNull Class<? extends R> getRecipeClass() {
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
    public static final Sprite ITEM_DROP = new Sprite(Constants.ITEM_DROP, 16, 16);
    public static final Sprite ELEM_INFUSE = new Sprite(Constants.ELEM_INFUSE, 16, 16);
    public static final Sprite BLOCK_PLACE = new Sprite(Constants.BLOCK_PLACE, 16, 16);
    public static final Sprite BLOCK_PLACE_UPON = new Sprite(Constants.BLOCK_PLACE_UPON, 16, 16);
    public static final Sprite LEFT_DARK = new Sprite(Constants.LEFT_DARK, 12, 12);
    public static final Sprite RIGHT_DARK = new Sprite(Constants.RIGHT_DARK, 12, 12);
    public static final Sprite UP_DARK = new Sprite(Constants.UP_DARK, 12, 12);
    public static final Sprite DOWN_DARK = new Sprite(Constants.DOWN_DARK, 12, 12);
    public static final Sprite LEFT_WHITE = new Sprite(Constants.LEFT_WHITE, 12, 12);
    public static final Sprite RIGHT_WHITE = new Sprite(Constants.RIGHT_WHITE, 12, 12);
    public static final Sprite UP_WHITE = new Sprite(Constants.UP_WHITE, 12, 12);
    public static final Sprite DOWN_WHITE = new Sprite(Constants.DOWN_WHITE, 12, 12);

    public static IDrawable toDrawable(ItemStack stack) {
        return JeiClient.getRuntime().getJeiHelpers().getGuiHelper().createDrawableItemStack(stack);
    }

    public static <T extends AbstractInputManager<T>> T add(IRecipeExtrasBuilder builder, T manager) {
        builder.addGuiEventListener(manager);
        builder.addWidget(manager);
        manager.onBuild();
        return manager;
    }

    /**
     * Texture sprite that is resolved through {@link mezz.jei.api.helpers.IGuiHelper} on first use,
     * so it can be declared statically before the JEI runtime is available.
     */
    public static final class Sprite implements IDrawable {
        private final Identifier id;
        private final int width;
        private final int height;
        private @Nullable IDrawable delegate;

        public Sprite(@NonNull Identifier id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }

        private @NonNull IDrawable delegate() {
            IDrawable resolved = this.delegate;
            if (resolved == null) {
                resolved = JeiClient.getRuntime().getJeiHelpers().getGuiHelper()
                    .createDrawable(this.id, 0, 0, this.width, this.height);
                this.delegate = resolved;
            }
            return resolved;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public void draw(@NonNull GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
            this.delegate().draw(guiGraphics, xOffset, yOffset);
        }
    }
}
