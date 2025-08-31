package cool.muyucloud.croparia.compat.jei.util;

import net.minecraft.client.gui.GuiGraphics;

public interface MousedDrawer {
    /**
     * Draw something at the given offset, with the given mouse position.
     *
     * @param guiGraphics The GuiGraphics to draw on.
     * @param guiOffsetX  The x offset relative to the recipe GUI.
     * @param guiOffsetY  The y offset relative to the recipe GUI.
     * @param guiMouseX   The x position of the mouse relative to the recipe GUI.
     * @param guiMouseY   The y position of the mouse relative to the recipe GUI.
     */
    void draw(GuiGraphics guiGraphics, int guiOffsetX, int guiOffsetY, double guiMouseX, double guiMouseY);
}
