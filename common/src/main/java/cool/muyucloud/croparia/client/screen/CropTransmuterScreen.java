package cool.muyucloud.croparia.client.screen;

import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.core.network.CropTransmuter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MaterialExtractorScreen extends AbstractContainerScreen<CropTransmuterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
        "textures/gui/container/dispenser.png"
    );
    private static final int BASE_WIDTH = 176;
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_CELL = 18;
    private static final int PANEL_COLS = 3;
    private static final int PANEL_ROWS = 3;
    private static final int PANEL_WIDTH = PANEL_COLS * PANEL_CELL + PANEL_PADDING * 2;

    private int page = 0;
    private Button prevButton;
    private Button nextButton;

    public MaterialExtractorScreen(CropTransmuterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = BASE_WIDTH + PANEL_WIDTH + 8;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int panelX = panelLeft();
        int panelY = panelTop() + imageHeight - 20;
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
            .pos(panelX, panelY)
            .size(20, 20)
            .build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
            .pos(panelX + 24, panelY)
            .size(20, 20)
            .build());
        updatePageButtons();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.blit(TEXTURE, left, top, 0, 0, BASE_WIDTH, this.imageHeight);
        renderSelectionPanel(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.croparia.material_extractor.select"), panelLeft() + 4, panelTop(), 0x404040, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updatePageButtons();
    }

    private void updatePageButtons() {
        int pages = getPageCount();
        if (page >= pages) page = Math.max(0, pages - 1);
        if (prevButton != null) prevButton.active = page > 0;
        if (nextButton != null) nextButton.active = page + 1 < pages;
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

    private int panelLeft() {
        return this.leftPos + BASE_WIDTH + 8;
    }

    private int panelTop() {
        return this.topPos + 6;
    }

    private void renderSelectionPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft();
        int top = panelTop() + 10;
        int width = PANEL_COLS * PANEL_CELL + PANEL_PADDING * 2;
        int height = PANEL_ROWS * PANEL_CELL + PANEL_PADDING * 2;
        graphics.fill(left, top, left + width, top + height, 0xAA1E1E1E);
        List<ItemStack> candidates = getCandidates();
        if (!menu.isSelectionRequired()) {
            graphics.drawString(
                this.font,
                Component.translatable("gui.croparia.material_extractor.auto_output"),
                left + PANEL_PADDING,
                top + PANEL_PADDING + 4,
                0xB0B0B0,
                false
            );
            return;
        }
        if (candidates.isEmpty()) {
            graphics.drawString(
                this.font,
                Component.translatable("gui.croparia.material_extractor.no_selection"),
                left + PANEL_PADDING,
                top + PANEL_PADDING + 4,
                0xB0B0B0,
                false
            );
            return;
        }
        int start = page * PANEL_COLS * PANEL_ROWS;
        int end = Math.min(start + PANEL_COLS * PANEL_ROWS, candidates.size());
        ResourceLocation selected = menu.getSelectedOutputId();
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % PANEL_COLS;
            int row = local / PANEL_COLS;
            int x = left + PANEL_PADDING + col * PANEL_CELL;
            int y = top + PANEL_PADDING + row * PANEL_CELL;
            ItemStack stack = candidates.get(i);
            graphics.renderItem(stack, x + 1, y + 1);
            ResourceLocation id = stack.getItem().arch$registryName();
            if (selected != null && selected.equals(id)) {
                graphics.fill(x, y, x + PANEL_CELL, y + PANEL_CELL, 0x66FFD54F);
            }
            if (mouseX >= x && mouseX < x + PANEL_CELL && mouseY >= y && mouseY < y + PANEL_CELL) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleSelectionClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSelectionClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!menu.isSelectionRequired()) return false;
        List<ItemStack> candidates = getCandidates();
        if (candidates.isEmpty()) return false;
        int left = panelLeft();
        int top = panelTop() + 10;
        int start = page * PANEL_COLS * PANEL_ROWS;
        int end = Math.min(start + PANEL_COLS * PANEL_ROWS, candidates.size());
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % PANEL_COLS;
            int row = local / PANEL_COLS;
            int x = left + PANEL_PADDING + col * PANEL_CELL;
            int y = top + PANEL_PADDING + row * PANEL_CELL;
            if (mouseX >= x && mouseX < x + PANEL_CELL && mouseY >= y && mouseY < y + PANEL_CELL) {
                ItemStack stack = candidates.get(i);
                ResourceLocation id = stack.getItem().arch$registryName();
                if (id != null && menu.getBlockPos() != null) {
                    new CropTransmuter(menu.getBlockPos(), id).send();
                }
                return true;
            }
        }
        return false;
    }

    private List<ItemStack> getCandidates() {
        return menu.getCandidateStacks();
    }
}
