package cool.muyucloud.croparia.compat.jei.util;

import cool.muyucloud.croparia.compat.jei.drawable.AbstractInputManager;

public interface MouseDragHandler<T extends AbstractInputManager<T>> {
    boolean handle(T manager, double mouseX, double mouseY, int button, double dragX, double dragY);

    interface NoReturn<T extends AbstractInputManager<T>> extends MouseDragHandler<T> {
        @Override
        default boolean handle(T manager, double mouseX, double mouseY, int button, double dragX, double dragY) {
            handleNoReturn(manager, mouseX, mouseY, button, dragX, dragY);
            return true;
        }

        void handleNoReturn(T manager, double mouseX, double mouseY, int button, double dragX, double dragY);
    }
}
