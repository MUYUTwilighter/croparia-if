package cool.muyucloud.croparia.compat.jei.drawable;

import cool.muyucloud.croparia.compat.jei.util.*;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.placement.IPlaceable;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class AbstractInputManager<T extends AbstractInputManager<T>> implements IJeiGuiEventListener, IPlaceable<T>, IDrawable {
    private ImmutableRect2i area = ImmutableRect2i.EMPTY;
    private final Map<String, MousedDrawer> drawables = new LinkedHashMap<>();
    private final Map<String, MouseScrollHandler<T>> onMouseScroll = new LinkedHashMap<>();
    private final Map<String, MouseKeyHandler<T>> onMouseClicked = new LinkedHashMap<>();
    private final Map<String, MouseKeyHandler<T>> onMouseReleased = new LinkedHashMap<>();
    private final Map<String, MouseDragHandler<T>> onMouseDragged = new LinkedHashMap<>();
    private final Map<String, MouseMoveHandler<T>> onMouseMoved = new LinkedHashMap<>();
    private final Map<String, MouseMoveHandler<T>> onMouseEntered = new LinkedHashMap<>();
    private final Map<String, MouseMoveHandler<T>> onMouseExited = new LinkedHashMap<>();
    private final Map<String, KeyboardHandler<T>> onKeyPressed = new LinkedHashMap<>();
    private double absoluteX = 0, absoluteY = 0;
    private boolean hovered = false;

    @SuppressWarnings("unchecked")
    public T getSelf() {
        return (T) this;
    }

    public void onBuild() {
    }

    public T addDrawable(String id, MousedDrawer drawer) {
        this.drawables.put(id, drawer);
        return this.getSelf();
    }

    public T addDrawable(String id, IDrawable drawable) {
        return this.addDrawable(id, (guiGraphics, xOffset, yOffset, mouseX, mouseY) ->
            drawable.draw(guiGraphics, xOffset, yOffset));
    }

    /**
     * Overloading method for adding a drawable using a Drawer.
     *
     * @param id       identifier for the drawable, used for later removal & updates
     * @param drawable functional interface to draw, see {@link Drawer}
     * @return this
     */
    public T addDrawable(String id, Drawer drawable) {
        return this.addDrawable(id, (IDrawable) drawable);
    }

    public T removeDrawable(String id) {
        this.drawables.remove(id);
        return this.getSelf();
    }

    public T clearDrawables() {
        this.drawables.clear();
        return this.getSelf();
    }

    public Stream<Map.Entry<String, MousedDrawer>> streamDrawables() {
        return this.drawables.entrySet().stream();
    }

    public double mouseX() {
        return MouseUtil.getX() - absoluteX();
    }

    public double parentMouseX() {
        return mouseX() + x();
    }

    public double mouseY() {
        return MouseUtil.getY() - absoluteY();
    }

    public double parentMouseY() {
        return mouseY() + y();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.onMouseScroll.values().stream().anyMatch(handler ->
            handler.handle(this.getSelf(), mouseX, mouseY, scrollDeltaX, scrollDeltaY)
        );
    }

    public T onMouseScrolled(String id, MouseScrollHandler<T> handler) {
        this.onMouseScroll.put(id, handler);
        return this.getSelf();
    }

    public T removeOnMouseScrolled(String id) {
        this.onMouseScroll.remove(id);
        return this.getSelf();
    }

    public T clearOnMouseScrolled() {
        this.onMouseScroll.clear();
        return this.getSelf();
    }

    public T onScrolledUp(String id, MouseScrollHandler<T> handler) {
        return onMouseScrolled(id, (manager, mouseX, mouseY, dx, dy) ->
            dy > 0 && handler.handle(manager.getSelf(), mouseX, mouseY, dx, dy));
    }

    public T onScrolledUp(String id, MouseScrollHandler.NoReturn<T> handler) {
        return onScrolledUp(id, (MouseScrollHandler<T>) handler);
    }

    public T onScrolledDown(String id, MouseScrollHandler<T> handler) {
        return onMouseScrolled(id, (manager, mouseX, mouseY, dx, dy) ->
            dy < 0 && handler.handle(manager.getSelf(), mouseX, mouseY, dx, dy));
    }

    public T onScrolledDown(String id, MouseScrollHandler.NoReturn<T> handler) {
        return onScrolledDown(id, (MouseScrollHandler<T>) handler);
    }

    public T onScrolledLeft(String id, MouseScrollHandler<T> handler) {
        return onMouseScrolled(id, (manager, mouseX, mouseY, dx, dy) ->
            dx < 0 && handler.handle(manager.getSelf(), mouseX, mouseY, dx, dy));
    }

    public T onScrolledLeft(String id, MouseScrollHandler.NoReturn<T> handler) {
        return onScrolledLeft(id, (MouseScrollHandler<T>) handler);
    }

    public T onScrolledRight(String id, MouseScrollHandler<T> handler) {
        return onMouseScrolled(id, (manager, mouseX, mouseY, dx, dy) ->
            dx > 0 && handler.handle(manager.getSelf(), mouseX, mouseY, dx, dy));
    }

    public T onScrolledRight(String id, MouseScrollHandler.NoReturn<T> handler) {
        return onScrolledRight(id, (MouseScrollHandler<T>) handler);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.onMouseDragged.values().stream().anyMatch(handler ->
            handler.handle(this.getSelf(), mouseX, mouseY, button, dragX, dragY)
        );
    }

    public T onMouseDragged(String id, MouseDragHandler<T> handler) {
        this.onMouseDragged.put(id, handler);
        return this.getSelf();
    }

    public T removeOnMouseDragged(String id) {
        this.onMouseDragged.remove(id);
        return this.getSelf();
    }

    public T clearOnMouseDragged() {
        this.onMouseDragged.clear();
        return this.getSelf();
    }

    public T onDragUp(String id, MouseDragHandler<T> handler) {
        return onMouseDragged(id, (manager, mouseX, mouseY, button, dx, dy) ->
            dy < 0 && handler.handle(manager, mouseX, mouseY, button, dx, dy));
    }

    public T onDragUp(String id, MouseDragHandler.NoReturn<T> handler) {
        return onDragUp(id, (MouseDragHandler<T>) handler);
    }

    public T onDragDown(String id, MouseDragHandler<T> handler) {
        return onMouseDragged(id, (manager, mouseX, mouseY, button, dx, dy) ->
            dy > 0 && handler.handle(manager, mouseX, mouseY, button, dx, dy));
    }

    public T onDragDown(String id, MouseDragHandler.NoReturn<T> handler) {
        return onDragDown(id, (MouseDragHandler<T>) handler);
    }

    public T onDragLeft(String id, MouseDragHandler<T> handler) {
        return onMouseDragged(id, (manager, mouseX, mouseY, button, dx, dy) ->
            dx < 0 && handler.handle(manager, mouseX, mouseY, button, dx, dy));
    }

    public T onDragLeft(String id, MouseDragHandler.NoReturn<T> handler) {
        return onDragLeft(id, (MouseDragHandler<T>) handler);
    }

    public T onDragRight(String id, MouseDragHandler<T> handler) {
        return onMouseDragged(id, (manager, mouseX, mouseY, button, dx, dy) ->
            dx > 0 && handler.handle(manager, mouseX, mouseY, button, dx, dy));
    }

    public T onDragRight(String id, MouseDragHandler.NoReturn<T> handler) {
        return onDragRight(id, (MouseDragHandler<T>) handler);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.onMouseClicked.values().stream().anyMatch(handler ->
            handler.handle(this.getSelf(), mouseX, mouseY, button)
        );
    }

    public T onClicked(String id, MouseKeyHandler<T> handler) {
        this.onMouseClicked.put(id, handler);
        return this.getSelf();
    }

    public T onClicked(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onClicked(id, (MouseKeyHandler<T>) handler);
    }

    public T removeOnClicked(String id) {
        this.onMouseClicked.remove(id);
        return this.getSelf();
    }

    public T clearOnClicked() {
        this.onMouseClicked.clear();
        return this.getSelf();
    }

    public T onLeftClicked(String id, MouseKeyHandler<T> handler) {
        return onClicked(id, (manager, mouseX, mouseY, button) ->
            button == 0 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onLeftClicked(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onLeftClicked(id, (MouseKeyHandler<T>) handler);
    }

    public T onRightClicked(String id, MouseKeyHandler<T> handler) {
        return onClicked(id, (manager, mouseX, mouseY, button) ->
            button == 1 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onRightClicked(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onRightClicked(id, (MouseKeyHandler<T>) handler);
    }

    public T onMiddleClicked(String id, MouseKeyHandler<T> handler) {
        return onClicked(id, (manager, mouseX, mouseY, button) ->
            button == 2 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onMiddleClicked(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onMiddleClicked(id, (MouseKeyHandler<T>) handler);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.onMouseReleased.values().stream().anyMatch(handler ->
            handler.handle(this.getSelf(), mouseX, mouseY, button)
        );
    }

    public T onReleased(String id, MouseKeyHandler<T> handler) {
        this.onMouseReleased.put(id, handler);
        return this.getSelf();
    }

    public T removeOnReleased(String id) {
        this.onMouseReleased.remove(id);
        return this.getSelf();
    }

    public T clearOnReleased() {
        this.onMouseReleased.clear();
        return this.getSelf();
    }

    public T onLeftReleased(String id, MouseKeyHandler<T> handler) {
        return onReleased(id, (manager, mouseX, mouseY, button) ->
            button == 0 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onLeftReleased(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onLeftReleased(id, (MouseKeyHandler<T>) handler);
    }

    public T onRightReleased(String id, MouseKeyHandler<T> handler) {
        return onReleased(id, (manager, mouseX, mouseY, button) ->
            button == 1 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onRightReleased(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onRightReleased(id, (MouseKeyHandler<T>) handler);
    }

    public T onMiddleReleased(String id, MouseKeyHandler<T> handler) {
        return onReleased(id, (manager, mouseX, mouseY, button) ->
            button == 2 && handler.handle(manager, mouseX, mouseY, button));
    }

    public T onMiddleReleased(String id, MouseKeyHandler.NoReturn<T> handler) {
        return onMiddleReleased(id, (MouseKeyHandler<T>) handler);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.absoluteX = MouseUtil.getX() - mouseX;
        this.absoluteY = MouseUtil.getY() - mouseY;
        //noinspection ResultOfMethodCallIgnored
        onMouseMoved.values().stream().anyMatch(handler -> handler.handle(this.getSelf(), mouseX, mouseY));
    }

    public T onMouseMoved(String id, MouseMoveHandler<T> handler) {
        this.onMouseMoved.put(id, handler);
        return this.getSelf();
    }

    public T removeOnMouseMoved(String id) {
        this.onMouseMoved.remove(id);
        return this.getSelf();
    }

    public T clearOnMouseMoved() {
        this.onMouseMoved.clear();
        return this.getSelf();
    }

    public T onMouseMoved(String id, MouseMoveHandler.NoReturn<T> handler) {
        return onMouseMoved(id, (MouseMoveHandler<T>) handler);
    }

    public T onMouseEntered(String id, MouseMoveHandler<T> handler) {
        this.onMouseEntered.put(id, handler);
        return this.getSelf();
    }

    public T removeOnMouseEntered(String id) {
        this.onMouseEntered.remove(id);
        return this.getSelf();
    }

    public T clearOnMouseEntered() {
        this.onMouseEntered.clear();
        return this.getSelf();
    }

    public T onMouseEntered(String id, MouseMoveHandler.NoReturn<T> handler) {
        return onMouseEntered(id, (MouseMoveHandler<T>) handler);
    }

    public T onMouseExited(String id, MouseMoveHandler<T> handler) {
        this.onMouseExited.put(id, handler);
        return this.getSelf();
    }

    public T removeOnMouseExited(String id) {
        this.onMouseExited.remove(id);
        return this.getSelf();
    }

    public T clearOnMouseExited() {
        this.onMouseExited.clear();
        return this.getSelf();
    }

    public T onMouseExited(String id, MouseMoveHandler.NoReturn<T> handler) {
        return onMouseExited(id, (MouseMoveHandler<T>) handler);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        return this.onKeyPressed.values().stream().anyMatch(handler ->
            handler.handle(this.getSelf(), mouseX, mouseY, keyCode, scanCode, modifiers)
        );
    }

    public T onKeyPressed(String id, KeyboardHandler<T> handler) {
        this.onKeyPressed.put(id, handler);
        return this.getSelf();
    }

    public T removeOnKeyPressed(String id) {
        this.onKeyPressed.remove(id);
        return this.getSelf();
    }

    public T clearOnKeyPressed() {
        this.onKeyPressed.clear();
        return this.getSelf();
    }

    public T onKeyPressed(String id, KeyboardHandler.NoReturn<T> handler) {
        return onKeyPressed(id, (KeyboardHandler<T>) handler);
    }

    public T onKeyPressed(String id, int keyCode, KeyboardHandler<T> handler) {
        return onKeyPressed(id, (manager, mouseX, mouseY, k, scanCode, modifiers) ->
            k == keyCode && handler.handle(manager, mouseX, mouseY, k, scanCode, modifiers));
    }

    public T onKeyPressed(String id, int keyCode, KeyboardHandler.NoReturn<T> handler) {
        return onKeyPressed(id, keyCode, (KeyboardHandler<T>) handler);
    }

    @Override
    public @NotNull ScreenRectangle getArea() {
        return area.toScreenRectangle();
    }

    @Override
    public @NotNull T setPosition(int x, int y) {
        area = area.setPosition(x, y);
        return this.getSelf();
    }

    public int x() {
        return area.x();
    }

    public double absoluteX() {
        return absoluteX;
    }

    public int y() {
        return area.y();
    }

    public double absoluteY() {
        return absoluteY;
    }

    public @NotNull T setSize(int width, int height) {
        area = new ImmutableRect2i(area.x(), area.y(), width, height);
        return this.getSelf();
    }

    @Override
    public int getWidth() {
        return area.getWidth();
    }

    @Override
    public int getHeight() {
        return area.getHeight();
    }

    @Override
    public void draw(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset) {
        double mouseX = mouseX();
        double mouseY = mouseY();
        if (0 < mouseX && mouseX < area.getWidth() && 0 < mouseY && mouseY < area.getHeight()) {
            if (!hovered) {
                hovered = true;
                //noinspection ResultOfMethodCallIgnored
                onMouseEntered.values().stream().anyMatch(handler -> handler.handle(this.getSelf(), mouseX, mouseY));
            }
        } else {
            if (hovered) {
                hovered = false;
                //noinspection ResultOfMethodCallIgnored
                onMouseExited.values().stream().anyMatch(handler -> handler.handle(this.getSelf(), mouseX, mouseY));
            }
        }
        this.drawables.values().forEach(drawer -> drawer.draw(
            guiGraphics, xOffset + x(), yOffset + y(), mouseX + xOffset + x(), mouseY + yOffset + y()
        ));
    }
}
