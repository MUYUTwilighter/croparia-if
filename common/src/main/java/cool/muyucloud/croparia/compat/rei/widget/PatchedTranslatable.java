package cool.muyucloud.croparia.compat.rei.widget;

import com.mojang.math.Transformation;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.DelegateWidget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

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
        return new Matrix4f(translate()).invert();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean containsMouse(double mouseX, double mouseY) {
        return super.containsMouse(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return super.charTyped(event);
    }

    @Override
    public double getZRenderingPriority() {
        Transformation transformation = new Transformation(translate());
        return transformation.getTranslation().z() + super.getZRenderingPriority();
    }

    @Override
    public Rectangle getBounds() {
        return super.getBounds();
    }
}
