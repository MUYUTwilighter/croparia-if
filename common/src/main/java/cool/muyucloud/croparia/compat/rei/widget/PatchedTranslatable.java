package cool.muyucloud.croparia.compat.rei.widget;

import com.mojang.math.Transformation;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.DelegateWidget;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.util.MatrixUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class PatchedTranslatable extends DelegateWidget {
    private final Supplier<Matrix4f> translate;

    public PatchedTranslatable(WidgetWithBounds widget, Supplier<Matrix4f> translate) {
        super(widget);
        this.translate = translate;
    }

    protected Matrix4f translate() {
        return translate.get();
    }

    protected final Matrix4f inverseTranslate() {
        return MatrixUtils.inverse(translate());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushPose();
        graphics.pose().last().pose().mul(translate());
        Vector4f mouse = transformMouse(mouseX, mouseY);
        super.render(graphics, (int) mouse.x(), (int) mouse.y(), delta);
        graphics.pose().popPose();
    }

    private Vector4f transformMouse(double mouseX, double mouseY) {
        Vector4f mouse = new Vector4f((float) mouseX, (float) mouseY, 0, 1);
        inverseTranslate().transform(mouse);
        return mouse;
    }

    @Override
    public boolean containsMouse(double mouseX, double mouseY) {
        Vector4f mouse = transformMouse(mouseX, mouseY);
        return super.containsMouse(mouse.x(), mouse.y());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vector4f mouse = transformMouse(mouseX, mouseY);
        mouseX = mouse.x();
        mouseY = mouse.y();
        Optional<GuiEventListener> optional = this.getChildAt(mouseX, mouseY);
        if (optional.isEmpty()) {
            return false;
        } else {
            GuiEventListener guiEventListener = optional.get();
            if (guiEventListener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(guiEventListener);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Vector4f mouse = transformMouse(mouseX, mouseY);
        return super.mouseReleased(mouse.x(), mouse.y(), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        Vector4f mouse = transformMouse(mouseX, mouseY);
        return super.mouseDragged(mouse.x(), mouse.y(), button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        Vector4f mouse = transformMouse(mouseX, mouseY);
        return super.mouseScrolled(mouse.x(), mouse.y(), amountX, amountY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        try {
            Widget.translateMouse(inverseTranslate());
            return super.keyPressed(keyCode, scanCode, modifiers);
        } finally {
            Widget.popMouse();
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        try {
            Widget.translateMouse(inverseTranslate());
            return super.keyReleased(keyCode, scanCode, modifiers);
        } finally {
            Widget.popMouse();
        }
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        try {
            Widget.translateMouse(inverseTranslate());
            return super.charTyped(character, modifiers);
        } finally {
            Widget.popMouse();
        }
    }

    @Override
    public double getZRenderingPriority() {
        Transformation transformation = new Transformation(translate());
        return transformation.getTranslation().z() + super.getZRenderingPriority();
    }

    @Override
    public Rectangle getBounds() {
        return MatrixUtils.transform(translate(), super.getBounds());
    }
}
