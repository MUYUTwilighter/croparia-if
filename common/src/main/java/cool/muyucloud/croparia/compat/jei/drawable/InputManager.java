package cool.muyucloud.croparia.compat.jei.drawable;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("UnusedReturnValue")
public class InputManager extends AbstractInputManager<InputManager> {
    private @Nullable Component tooltip;

    public static InputManager createButton(IDrawable hover, IDrawable notHover) {
        return new InputManager().addDrawable("img", notHover)
            .onMouseExited("img", (manager, mouseX, mouseY) -> manager.addDrawable("img", notHover))
            .onMouseEntered("img", (manager, mouseX, mouseY) -> manager.addDrawable("img", hover));
    }

    public InputManager setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
        if (this.tooltip != null) {
            tooltipBuilder.add(this.tooltip);
        }
    }

    @Override
    public InputManager getSelf() {
        return this;
    }
}
