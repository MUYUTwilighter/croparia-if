package cool.muyucloud.croparia.compat.jei.category;

import com.mojang.blaze3d.platform.InputConstants;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.jei.drawable.Drawer;
import cool.muyucloud.croparia.compat.jei.drawable.DynamicSlot;
import cool.muyucloud.croparia.compat.jei.drawable.InputManager;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.RangedVec3i;
import cool.muyucloud.croparia.util.Ref;
import cool.muyucloud.croparia.util.Vec2i;
import cool.muyucloud.croparia.util.supplier.SemiSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JeiRitualStructure extends JeiCategory<RitualStructure> {
    public static final JeiRitualStructure INSTANCE = new JeiRitualStructure();
    public static final Vec2i MAX_DISPLAY_SIZE = new Vec2i(6, 6);

    @Override
    public TypedSerializer<RitualStructure> getTypedSerializer() {
        return RitualStructure.TYPED_SERIALIZER;
    }

    @Override
    public int getWidth() {
        return (MAX_DISPLAY_SIZE.x() + 2) * SLOT_SIZE;
    }

    @Override
    public int getHeight() {
        return (MAX_DISPLAY_SIZE.z() + 3) * SLOT_SIZE;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return toDrawable(CropariaItems.RITUAL_STAND.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull RitualStructure recipe, @NotNull IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(recipe.getRitual());
        for (int x = 0; x < recipe.size().getX(); x++) {
            for (int y = 0; y < recipe.size().getY(); y++) {
                for (int z = 0; z < recipe.size().getZ(); z++) {
                    char key = recipe.getPattern().get(x, y, z);
                    if (key == '*')
                        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(recipe.getRitual());
                    else if (key != ' ' && key != '.' && key != '$')
                        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(recipe.getKeys().get(key));
                }
            }
        }
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, @NotNull RitualStructure recipe, @NotNull IFocusGroup focuses) {
        Vec3i displaySize = new Vec3i(
            Math.min(recipe.size().getX(), MAX_DISPLAY_SIZE.x()),
            recipe.size().getY(),
            Math.min(recipe.size().getZ(), MAX_DISPLAY_SIZE.z())
        );
        if (displaySize.getX() == 0 || displaySize.getY() == 0 || displaySize.getZ() == 0) return;
        Ref<RangedVec3i> anchor = new Ref<>(RangedVec3i.maxBounds(
            recipe.size().getX() - displaySize.getX(), recipe.size().getY() - 1,
            recipe.size().getZ() - displaySize.getZ()));
        InputManager inputManager = new InputManager().setSize(this.getWidth(), this.getHeight());
        inputManager.onScrolledUp("y-", (manager, mouseX, mouseY, scrollDeltaX, scrollDeltaY) -> {
            return anchor.mapAndCompare(RangedVec3i::below);
        }).onScrolledDown("y+", (manager, mouseX, mouseY, scrollDeltaX, scrollDeltaY) -> {
            return anchor.mapAndCompare(RangedVec3i::above);
        }).onKeyPressed("z-", InputConstants.KEY_W, (manager, mouseX, mouseY, keyCode, scanCode, modifiers) -> {
            return anchor.mapAndCompare(RangedVec3i::north);
        }).onKeyPressed("z+", InputConstants.KEY_S, (manager, mouseX, mouseY, keyCode, scanCode, modifiers) -> {
            return anchor.mapAndCompare(RangedVec3i::south);
        }).onKeyPressed("x-", InputConstants.KEY_A, (manager, mouseX, mouseY, keyCode, scanCode, modifiers) -> {
            return anchor.mapAndCompare(RangedVec3i::west);
        }).onKeyPressed("x+", InputConstants.KEY_D, (manager, mouseX, mouseY, keyCode, scanCode, modifiers) -> {
            return anchor.mapAndCompare(RangedVec3i::east);
        });
        add(builder, InputManager.createButton(LEFT_WHITE, LEFT_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition(SLOT_SIZE, this.getHeight() - (BUTTON_SIZE + SLOT_SIZE) / 2)
            .onLeftClicked("y-", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dy(-1)));
        add(builder, InputManager.createButton(RIGHT_WHITE, RIGHT_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition(this.getWidth() - SLOT_SIZE - BUTTON_SIZE, this.getHeight() - (BUTTON_SIZE + SLOT_SIZE) / 2)
            .onClicked("y+", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dy(1)));
        add(builder, InputManager.createButton(LEFT_WHITE, LEFT_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition((SLOT_SIZE - BUTTON_SIZE) / 2, displaySize.getZ() * SLOT_SIZE / 2 + SLOT_SIZE - BUTTON_SIZE / 2)
            .onClicked("x-", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dx(-1)));
        add(builder, InputManager.createButton(RIGHT_WHITE, RIGHT_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition(displaySize.getX() * SLOT_SIZE + SLOT_SIZE + (SLOT_SIZE - BUTTON_SIZE) / 2,
                ((displaySize.getZ() + 2) * SLOT_SIZE - BUTTON_SIZE) / 2)
            .onClicked("x+", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dx(1)));
        add(builder, InputManager.createButton(UP_WHITE, UP_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition(displaySize.getX() * SLOT_SIZE / 2 + SLOT_SIZE - BUTTON_SIZE / 2, (SLOT_SIZE - BUTTON_SIZE) / 2)
            .onClicked("z-", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dz(-1)));
        add(builder, InputManager.createButton(DOWN_WHITE, DOWN_DARK)).setSize(BUTTON_SIZE, BUTTON_SIZE)
            .setPosition(displaySize.getX() * SLOT_SIZE / 2 + SLOT_SIZE - BUTTON_SIZE / 2,
                this.getHeight() - SLOT_SIZE - (BUTTON_SIZE + SLOT_SIZE) / 2)
            .onClicked("z+", (manager, mouseX, mouseY, button) -> anchor.set(anchor.get().dz(1)));
        List<Drawer> tooltips = new ArrayList<>();
        for (int x = 0; x < displaySize.getX(); x++) {
            for (int z = 0; z < displaySize.getZ(); z++) {
                int finalX = x;
                int finalZ = z;
                SemiSupplier<List<ItemStack>> dynamicStacks = SemiSupplier.of(() ->
                    recipe.displaySlot(finalX + anchor.get().getX(), anchor.get().getY(), finalZ + anchor.get().getZ()));
                DynamicSlot slot = new DynamicSlot(inst -> {
                    if (recipe.isVirtualRender(finalX + anchor.get().getX(), anchor.get().getY(), finalZ + anchor.get().getZ())) {
                        inst.disableClick();
                        inst.disableHighlight();
                    } else {
                        inst.enableClick();
                        inst.enableHighlight();
                    }
                    return dynamicStacks.get();
                }).setPosition(SLOT_SIZE * (x + 1), SLOT_SIZE * (z + 1));
                tooltips.add(slot.tooltipDrawer());
                add(builder, slot);
                anchor.onChanged((old, value) -> {
                    if (!old.equals(value)) dynamicStacks.refresh();
                });
            }
        }
        SemiSupplier<Component> label = SemiSupplier.of(() -> Texts.translatable("gui.croparia.ritual_structure.label", anchor.get().getY() + 1));
        anchor.onChanged((old, value) -> {
            if (old.getY() != value.getY()) label.refresh();
        });
        //noinspection DataFlowIssue
        builder.addDrawable(Drawer.of((guiGraphics, xOffset, yOffset) -> guiGraphics.drawCenteredString(
            Minecraft.getInstance().font, label.get(), this.getWidth() / 2, SLOT_SIZE * (displaySize.getZ() + 2) + BUTTON_SIZE / 2,
            ChatFormatting.WHITE.getColor()
        )));
        builder.addDrawable(Drawer.of((guiGraphics, xOffset, yOffset) ->
            tooltips.forEach(drawer -> drawer.draw(guiGraphics, xOffset, yOffset))));
        add(builder, inputManager);
    }
}
