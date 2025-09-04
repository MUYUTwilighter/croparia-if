package cool.muyucloud.croparia.compat.rei.widget;

import me.shedaniel.clothconfig2.api.animator.NumberAnimator;
import me.shedaniel.clothconfig2.api.animator.ValueAnimator;
import me.shedaniel.clothconfig2.api.scroll.ScrollingContainer;
import me.shedaniel.math.FloatingPoint;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.widgets.CloseableScissors;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

@SuppressWarnings("UnstableApiUsage")
public class PatchedOverflow extends PatchedTranslatable {
    private final Rectangle bounds;
    private final NumberAnimator<Float> scale;
    private final ValueAnimator<FloatingPoint> translate;
    private final ValueAnimator<FloatingPoint> velocity;
    private double draggedX = 0, draggedY = 0;

    public PatchedOverflow(Rectangle bounds, WidgetWithBounds widget) {
        super(widget, Matrix4f::new);
        this.bounds = bounds;
        this.scale = ValueAnimator.ofFloat()
            .setAs(1f);
        this.translate = ValueAnimator.ofFloatingPoint()
            .setAs(new FloatingPoint(-widget.getBounds().width / 2f, -bounds.height / 2f));
        this.velocity = ValueAnimator.ofFloatingPoint()
            .setAs(new FloatingPoint(0f, 0f));
    }

    @Override
    protected Matrix4f translate() {
        FloatingPoint translate = this.translate.value();
        float scale = 1 / Math.max(this.scale.floatValue(), 0.001f);
        Matrix4f matrix = new Matrix4f().translate(bounds.getCenterX() + (float) translate.x * scale, bounds.getCenterY() + (float) translate.y * scale, 0);
        matrix.mul(new Matrix4f().scale(scale, scale, 1));
        return matrix;
    }

    @Override
    @SuppressWarnings("unused")
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Rectangle widgetBounds = ((WidgetWithBounds) delegate()).getBounds();
        this.scale.update(delta);
        this.scale.setTarget(ScrollingContainer.handleBounceBack(this.scale.target() - 0.78,
            Math.min(widgetBounds.width * 1.0F / getBounds().width, widgetBounds.height * 1.0F / getBounds().height) - 0.78, delta, .001) + 0.78);
        this.translate.update(delta);
        for (int i = 0; i < 3; i++) {
            this.translate.setAs(new FloatingPoint(
                ScrollingContainer.handleBounceBack(this.translate.target().x + widgetBounds.width - (double) getBounds().width / 2 * scale.value(),
                    widgetBounds.width - getBounds().width * scale.value(), delta, .0001) - (widgetBounds.width - (double) getBounds().width / 2 * scale.value()),
                ScrollingContainer.handleBounceBack(this.translate.target().y + widgetBounds.height - (double) getBounds().height / 2 * scale.value(),
                    widgetBounds.height - getBounds().height * scale.value(), delta, .0001) - (widgetBounds.height - (double) getBounds().height / 2 * scale.value())
            ));
        }
        if (!isDragging()) {
            this.translate.setAs(new FloatingPoint(this.translate.value().x + this.velocity.value().x, this.translate.value().y + this.velocity.value().y));
        }
        this.velocity.update(delta);
        this.velocity.setTo(new FloatingPoint(
            ScrollingContainer.handleBounceBack(this.velocity.target().x, 0, delta, .0001),
            ScrollingContainer.handleBounceBack(this.velocity.target().y, 0, delta, .0001)
        ), ConfigObject.getInstance().isReducedMotion() ? 0 : 20);

        try (CloseableScissors scissors = scissor(graphics, this.bounds)) {
            boolean containsMouse = this.bounds.contains(mouseX, mouseY);

            if (containsMouse) {
                super.render(graphics, mouseX, mouseY, delta);
            } else {
                super.render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, delta);
            }
        }
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public boolean containsMouse(double mouseX, double mouseY) {
        return getBounds().contains(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (containsMouse(mouseX, mouseY) && amountY != 0) {
            this.scale.setTo(this.scale.target() + amountY * -0.2f, ConfigObject.getInstance().isReducedMotion() ? 0 : 300);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (containsMouse(mouseX, mouseY)) {
            if (button == 0) {
                setDragging(true);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging() && button == 0) {
            setDragging(false);
        }
        if (draggedX < 1 && draggedY < 1) {
            draggedX = 0;
            draggedY = 0;
            return super.mouseReleased(mouseX, mouseY, button);
        }
        draggedX = 0;
        draggedY = 0;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        draggedX += deltaX;
        draggedY += deltaY;
        if (isDragging() && button == 0) {
            double newXTranslate = translate.target().x;
            double newYTranslate = translate.target().y;
            newXTranslate += deltaX * scale.doubleValue();
            newYTranslate += deltaY * scale.doubleValue();

            translate.setAs(new FloatingPoint(newXTranslate, newYTranslate));
            velocity.setAs(new FloatingPoint(deltaX * scale.doubleValue(), deltaY * scale.doubleValue()));
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
