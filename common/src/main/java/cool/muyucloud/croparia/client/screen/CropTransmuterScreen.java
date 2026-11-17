package cool.muyucloud.croparia.client.screen;

import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.core.network.CropTransmuterRedstoneModePacket;
import cool.muyucloud.croparia.api.core.network.CropTransmuterSelectPacket;
import cool.muyucloud.croparia.api.crop.util.Material;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CropTransmuterScreen extends AbstractContainerScreen<CropTransmuterMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace(
        "textures/gui/container/dispenser.png"
    );
    private static final int PANEL_CELL = 18;
    private static final int PANEL_COLS = 3;
    private static final int PANEL_ROWS = 3;
    private static final int SELECTION_X = 62;
    private static final int SELECTION_Y = 17;
    private static final int INPUT_SLOT_X = 19;
    private static final int INPUT_SLOT_Y = 34;
    private static final int OUTPUT_SLOT_X = 137;
    private static final int OUTPUT_SLOT_Y = 34;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_OUTER_BORDER = 0xFF5B4A36;
    private static final int PANEL_BACKGROUND = 0xFF221A12;
    private static final int PANEL_CELL_BACKGROUND = 0xFF382B1E;
    private static final int PANEL_CELL_HOVER = 0x66FFF2CC;
    private static final int PANEL_CELL_SELECTED = 0xCCFFD54F;
    private static final int SLOT_BORDER = 0xFF8B6B43;
    private static final int SLOT_BACKGROUND = 0xFF2A2017;
    private static final int PANEL_TEXT = 0xE8D9C2;
    private static final int PANEL_TEXT_MUTED = 0xBFAF96;

    private int page = 0;
    private Button prevButton;
    private Button nextButton;
    private Button redstoneModeButton;
    private boolean localPositiveRedstone;

    public CropTransmuterScreen(CropTransmuterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.localPositiveRedstone = menu.isPositiveRedstone();
    }

    @Override
    protected void init() {
        super.init();
        int panelX = this.leftPos + SELECTION_X;
        int panelY = this.topPos + SELECTION_Y + PANEL_ROWS * PANEL_CELL + 4;
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
            .pos(panelX, panelY)
            .size(18, 20)
            .build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
            .pos(panelX + PANEL_COLS * PANEL_CELL - 18, panelY)
            .size(18, 20)
            .build());
        redstoneModeButton = addRenderableWidget(Button.builder(getRedstoneModeLabel(), button -> toggleRedstoneMode())
            .pos(this.leftPos + this.imageWidth - 24, this.topPos + 4)
            .size(20, 20)
            .build());
        updatePageButtons();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        renderMachineSlots(graphics);
        renderSelectionPanel(graphics, mouseX, mouseY);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderSelectionTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        drawPanelFooter(graphics);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updatePageButtons();
        this.localPositiveRedstone = this.menu.isPositiveRedstone();
        if (redstoneModeButton != null) {
            redstoneModeButton.setMessage(getRedstoneModeLabel());
        }
    }

    private void updatePageButtons() {
        int pages = getPageCount();
        if (page >= pages) page = Math.max(0, pages - 1);
        boolean showPaging = pages > 1;
        if (prevButton != null) {
            prevButton.visible = showPaging;
            prevButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.visible = showPaging;
            nextButton.active = page + 1 < pages;
        }
    }

    private int getPageCount() {
        int size = getCandidates().size();
        if (size == 0) return 1;
        return (size + (PANEL_COLS * PANEL_ROWS - 1)) / (PANEL_COLS * PANEL_ROWS);
    }

    private void changePage(int delta) {
        int pages = getPageCount();
        page = Math.max(0, Math.min(page + delta, pages - 1));
        updatePageButtons();
    }

    private void renderSelectionPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = this.leftPos + SELECTION_X;
        int top = this.topPos + SELECTION_Y;
        int width = PANEL_COLS * PANEL_CELL;
        int height = PANEL_ROWS * PANEL_CELL;
        graphics.fill(left - 2, top - 2, left + width + 2, top + height + 2, PANEL_OUTER_BORDER);
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, PANEL_BACKGROUND);
        List<ItemStack> candidates = getCandidates();
        if (menu.getBlockEntity().readInputMaterial().isEmpty()) {
            drawPanelMessage(graphics, Component.translatable("gui.croparia.crop_transmuter.no_input"), left, top, width, height);
            return;
        }
        if (candidates.isEmpty()) {
            drawPanelMessage(graphics, Component.translatable("gui.croparia.crop_transmuter.no_selection"), left, top, width, height);
            return;
        }
        int hovered = getHoveredCandidateIndex(mouseX, mouseY, candidates.size());
        int start = getVisibleStart();
        int end = Math.min(start + PANEL_COLS * PANEL_ROWS, candidates.size());
        Identifier selected = this.getSelectedStack().getItem().arch$registryName();
        for (int local = 0; local < PANEL_COLS * PANEL_ROWS; local++) {
            int col = local % PANEL_COLS;
            int row = local / PANEL_COLS;
            int x = left + col * PANEL_CELL;
            int y = top + row * PANEL_CELL;
            graphics.fill(x, y, x + PANEL_CELL - 1, y + PANEL_CELL - 1, PANEL_CELL_BACKGROUND);
        }
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % PANEL_COLS;
            int row = local / PANEL_COLS;
            int x = left + col * PANEL_CELL;
            int y = top + row * PANEL_CELL;
            ItemStack stack = candidates.get(i);
            if (hovered == i) {
                graphics.fill(x, y, x + PANEL_CELL - 1, y + PANEL_CELL - 1, PANEL_CELL_HOVER);
            }
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
            Identifier id = stack.getItem().arch$registryName();
            if (selected != null && selected.equals(id)) {
                graphics.renderOutline(x, y, PANEL_CELL, PANEL_CELL, PANEL_CELL_SELECTED);
            }
        }
    }

    private void renderSelectionTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.getMenu().getBlockEntity().readInputMaterial().isEmpty()) return;
        List<ItemStack> candidates = getCandidates();
        if (candidates.isEmpty()) return;
        int hovered = getHoveredCandidateIndex(mouseX, mouseY, candidates.size());
        if (hovered >= 0 && hovered < candidates.size()) {
            graphics.setTooltipForNextFrame(this.font, candidates.get(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (handleSelectionClick(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean handleSelectionClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (this.getMenu().getBlockEntity().readInputMaterial().isEmpty()) return false;
        List<ItemStack> candidates = getCandidates();
        if (candidates.isEmpty()) return false;
        int hovered = getHoveredCandidateIndex(mouseX, mouseY, candidates.size());
        if (hovered >= 0 && hovered < candidates.size()) {
            ItemStack stack = candidates.get(hovered);
            if (stack.getItem().arch$registryName() != null) {
                new CropTransmuterSelectPacket(this.getMenu().getBlockEntity().getBlockPos(), hovered).send();
            }
            return true;
        }
        return false;
    }

    private void toggleRedstoneMode() {
        this.localPositiveRedstone = !this.localPositiveRedstone;
        if (redstoneModeButton != null) {
            redstoneModeButton.setMessage(getRedstoneModeLabel());
        }
        new CropTransmuterRedstoneModePacket(this.getMenu().getBlockPos()).send();
    }

    private Component getRedstoneModeLabel() {
        return Component.translatable(
            this.localPositiveRedstone
                ? "gui.croparia.crop_transmuter.redstone_positive"
                : "gui.croparia.crop_transmuter.redstone_negative"
        );
    }

    private void drawPanelFooter(GuiGraphics graphics) {
        List<ItemStack> candidates = getCandidates();
        if (candidates.size() <= 1) {
            return;
        }
        Component footer = Component.literal((page + 1) + "/" + getPageCount());
        int x = SELECTION_X + (PANEL_COLS * PANEL_CELL - this.font.width(footer)) / 2;
        int y = SELECTION_Y + PANEL_ROWS * PANEL_CELL + 10;
        graphics.drawString(this.font, footer, x, y, PANEL_TEXT, false);
    }

    private void drawPanelMessage(GuiGraphics graphics, Component message, int left, int top, int width, int height) {
        int textX = left + (width - this.font.width(message)) / 2;
        int textY = top + height / 2 - 4;
        graphics.drawString(this.font, message, textX, textY, PANEL_TEXT_MUTED, false);
    }

    private void renderMachineSlots(GuiGraphics graphics) {
        drawSlotFrame(graphics, this.leftPos + INPUT_SLOT_X, this.topPos + INPUT_SLOT_Y);
        drawSlotFrame(graphics, this.leftPos + OUTPUT_SLOT_X, this.topPos + OUTPUT_SLOT_Y);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, SLOT_BORDER);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND);
        graphics.renderOutline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, SLOT_BORDER);
    }

    private int getVisibleStart() {
        return page * PANEL_COLS * PANEL_ROWS;
    }

    private int getHoveredCandidateIndex(double mouseX, double mouseY, int candidateCount) {
        int left = this.leftPos + SELECTION_X;
        int top = this.topPos + SELECTION_Y;
        if (mouseX < left || mouseX >= left + PANEL_COLS * PANEL_CELL || mouseY < top || mouseY >= top + PANEL_ROWS * PANEL_CELL) {
            return -1;
        }
        int col = (int) ((mouseX - left) / PANEL_CELL);
        int row = (int) ((mouseY - top) / PANEL_CELL);
        int index = getVisibleStart() + row * PANEL_COLS + col;
        return index < candidateCount ? index : -1;
    }

    private List<ItemStack> getCandidates() {
        return menu.getBlockEntity().readInputMaterial().map(Material::asItems).orElse(List.of());
    }

    private ItemStack getSelectedStack() {
        Optional<Material<?>> mayMaterial = this.getMenu().getBlockEntity().readInputMaterial();
        if (mayMaterial.isEmpty()) return ItemStack.EMPTY;
        List<ItemStack> stacks = mayMaterial.get().asItems();
        return stacks.get(Math.min(stacks.size() - 1, this.getMenu().getBlockEntity().getSelectedIndex()));
    }
}
