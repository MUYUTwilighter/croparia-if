package cool.muyucloud.croparia.compat.jei.util;

import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;

public interface MouseScrollHandler<T extends AbstractInputManager<T>> {
    boolean handle(T manager, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY);

    interface NoReturn<T extends AbstractInputManager<T>> extends MouseScrollHandler<T> {
        default boolean handle(T manager, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            handleNoReturn(manager, mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            return true;
        }

        void handleNoReturn(T manager, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY);
    }
}
