package cool.muyucloud.croparia.compat.jei.drawable;

import mezz.jei.api.gui.drawable.IDrawable;

public interface Drawer extends IDrawable {
    static Drawer of(Drawer drawer) {
        return drawer;
    }

    @Override
    default int getHeight() {
        return 0;
    }

    @Override
    default int getWidth() {
        return 0;
    }
}
