package cool.muyucloud.croparia.compat.jei.util;

import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;

public interface MouseKeyHandler<T extends AbstractInputManager<T>> {
    boolean handle(T manager, double mouseX, double mouseY, int button);

    interface NoReturn<T extends AbstractInputManager<T>> extends MouseKeyHandler<T> {
        @Override
        default boolean handle(T manager, double mouseX, double mouseY, int button) {
            handleNoReturn(manager, mouseX, mouseY, button);
            return true;
        }

        void handleNoReturn(T manager, double mouseX, double mouseY, int button);
    }
}
