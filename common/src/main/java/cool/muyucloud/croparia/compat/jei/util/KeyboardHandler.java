package cool.muyucloud.croparia.compat.jei.util;

import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;

public interface KeyboardHandler<T extends AbstractInputManager<T>> {
    boolean handle(T manager, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers);

    interface NoReturn<T extends AbstractInputManager<T>> extends KeyboardHandler<T> {
        @Override
        default boolean handle(T manager, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
            handleNoReturn(manager, mouseX, mouseY, keyCode, scanCode, modifiers);
            return true;
        }

        void handleNoReturn(T manager, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers);
    }
}
