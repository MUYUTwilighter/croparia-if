package cool.muyucloud.croparia.compat.jei.drawable;

import cool.muyucloud.croparia.compat.jei.JeiClient;
import cool.muyucloud.croparia.compat.jei.category.JeiCategory;
import cool.muyucloud.croparia.compat.jei.util.MouseKeyHandler;
import cool.muyucloud.croparia.compat.jei.util.MouseMoveHandler;
import cool.muyucloud.croparia.util.CifUtil;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class DynamicSlot extends AbstractInputManager<DynamicSlot> {
    public static final MouseKeyHandler.NoReturn<DynamicSlot> SHOW_RECIPE = (manager, mouseX, mouseY, button) ->
        JeiClient.getRuntime().getRecipesGui().show(focusStack(RecipeIngredientRole.OUTPUT, manager.getCurrentStack()));
    public static final MouseKeyHandler.NoReturn<DynamicSlot> SHOW_USAGE = (manager, mouseX, mouseY, button) ->
        JeiClient.getRuntime().getRecipesGui().show(focusStack(RecipeIngredientRole.INPUT, manager.getCurrentStack()));
    public static final MouseMoveHandler.NoReturn<DynamicSlot> HIGHLIGHT = (manager, a, b) -> manager.addDrawable(
        "highlight", (guiGraphics, xOffset, yOffset) -> guiGraphics.fill(
            RenderType.guiOverlay(), xOffset + 1, yOffset + 1, xOffset + JeiCategory.SLOT_SIZE_HIGHLIGHT,
            yOffset + JeiCategory.SLOT_SIZE_HIGHLIGHT, 1, 0x80FFFFFF));
    public static final MouseMoveHandler.NoReturn<DynamicSlot> CLEAR = (manager, a, b) -> manager.removeDrawable("highlight");

    public static IFocus<ItemStack> focusStack(RecipeIngredientRole role, ItemStack stack) {
        return JeiClient.getRuntime().getJeiHelpers().getFocusFactory().createFocus(role, JeiCategory.toIngredient(stack));
    }

    private final Function<DynamicSlot, List<ItemStack>> stacks;

    public DynamicSlot(Function<DynamicSlot, List<ItemStack>> stacks) {
        this.stacks = stacks;
        this.setSize(JeiCategory.SLOT_SIZE, JeiCategory.SLOT_SIZE);
        this.addDrawable("background", (guiGraphics, xOffset, yOffset, mouseX, mouseY) ->
            JeiClient.getRuntime().getJeiHelpers().getGuiHelper().getSlotDrawable().draw(guiGraphics, xOffset, yOffset));
        this.addDrawable("item", (guiGraphics, xOffset, yOffset, mouseX, mouseY) ->
            this.getCurrentIngredient().draw(guiGraphics, xOffset + 1, yOffset + 1));
        this.onLeftClicked("showRecipe", SHOW_RECIPE);
        this.onRightClicked("showUsage", SHOW_USAGE);
        this.onMouseEntered("highlight", HIGHLIGHT);
        this.onMouseExited("clear", CLEAR);
    }

    public Drawer tooltipDrawer() {
        return (guiGraphics, xOffset, yOffset) -> {
            double mouseX = mouseX();
            double mouseY = mouseY();
            if (0 < mouseX && mouseX < this.getWidth() && 0 < mouseY && mouseY < this.getHeight()) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, this.getCurrentStack(), CifUtil.toIntSafe(parentMouseX()), CifUtil.toIntSafe(parentMouseY()));
            }
        };
    }

    public ItemStack getCurrentStack() {
        List<ItemStack> items = this.stacks.apply(this);
        if (items.isEmpty()) return ItemStack.EMPTY;
        else if (items.size() == 1) return items.getFirst();
        else return items.get(CifUtil.toIntSafe((System.currentTimeMillis() / 1000) % items.size()));
    }

    public IDrawable getCurrentIngredient() {
        return JeiCategory.toDrawable(this.getCurrentStack());
    }

    @Override
    public DynamicSlot getSelf() {
        return this;
    }

    public void disableClick() {
        this.removeOnClicked("showRecipe");
        this.removeOnClicked("showUsage");
    }

    public void enableClick() {
        this.onLeftClicked("showRecipe", SHOW_RECIPE);
        this.onRightClicked("showUsage", SHOW_USAGE);
    }

    public void disableHighlight() {
        this.removeOnMouseEntered("highlight");
        this.removeOnMouseExited("clear");
    }

    public void enableHighlight() {
        this.onMouseEntered("highlight", HIGHLIGHT);
        this.onMouseExited("clear", CLEAR);
    }
}
