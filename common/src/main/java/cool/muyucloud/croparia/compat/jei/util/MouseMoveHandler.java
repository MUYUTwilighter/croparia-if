package cool.muyucloud.croparia.compat.jei.util;

import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;

public interface MouseMoveHandler<T extends AbstractInputManager<T>> {
    boolean handle(T manager, double mouseX, double mouseY);

    interface NoReturn<T extends AbstractInputManager<T>> extends MouseMoveHandler<T> {
        @Override
        default boolean handle(T manager, double mouseX, double mouseY) {
            handleNoReturn(manager, mouseX, mouseY);
            return true;
        }

        void handleNoReturn(T manager, double mouseX, double mouseY);
    }
}
