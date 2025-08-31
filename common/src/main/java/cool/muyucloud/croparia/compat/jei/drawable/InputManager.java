package cool.muyucloud.croparia.compat.jei.drawable;

import mezz.jei.api.gui.drawable.IDrawable;

@SuppressWarnings("UnusedReturnValue")
public class InputManager extends AbstractInputManager<InputManager> {
    public static InputManager createButton(IDrawable hover, IDrawable notHover) {
        return new InputManager().addDrawable("img", notHover)
            .onMouseExited("img", (manager, mouseX, mouseY) -> manager.addDrawable("img", notHover))
            .onMouseEntered("img", (manager, mouseX, mouseY) -> manager.addDrawable("img", hover));
    }

    @Override
    public InputManager getSelf() {
        return this;
    }
}
