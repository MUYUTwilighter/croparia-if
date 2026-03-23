package cool.muyucloud.croparia.api.core.menu;

import cool.muyucloud.croparia.api.core.block.entity.MaterialExtractorBlockEntity;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.registry.MenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MaterialExtractorMenu extends AbstractContainerMenu {
    private final Container container;
    private final @Nullable MaterialExtractorBlockEntity blockEntity;
    private final BlockPos pos;

    public MaterialExtractorMenu(int syncId, Inventory inventory, MaterialExtractorBlockEntity blockEntity) {
        this(syncId, inventory, blockEntity, blockEntity.getBlockPos());
    }

    public MaterialExtractorMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, resolveBlockEntity(inventory, buf));
    }

    private MaterialExtractorMenu(int syncId, Inventory inventory, Resolved resolved) {
        this(syncId, inventory, resolved.blockEntity, resolved.pos);
    }

    private MaterialExtractorMenu(int syncId, Inventory inventory, @Nullable MaterialExtractorBlockEntity blockEntity, BlockPos pos) {
        super(MenuTypes.MATERIAL_EXTRACTOR.get(), syncId);
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.container = blockEntity == null ? new SimpleContainer(MaterialExtractorBlockEntity.INVENTORY_SIZE) : blockEntity;
        checkContainerSize(this.container, MaterialExtractorBlockEntity.INVENTORY_SIZE);
        this.container.startOpen(inventory.player);
        addSlots(inventory);
    }

    private static Resolved resolveBlockEntity(Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MaterialExtractorBlockEntity extractor) {
            return new Resolved(extractor, pos);
        }
        return new Resolved(null, pos);
    }

    private record Resolved(@Nullable MaterialExtractorBlockEntity blockEntity, BlockPos pos) {}

    private void addSlots(Inventory playerInventory) {
        this.addSlot(new Slot(container, MaterialExtractorBlockEntity.INPUT_SLOT, 26, 18));
        int outputStart = MaterialExtractorBlockEntity.OUTPUT_START;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = outputStart + col + row * 3;
                int x = 62 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new Slot(container, index, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        itemStack = stack.copy();
        int containerSlots = MaterialExtractorBlockEntity.INVENTORY_SIZE;
        if (index < containerSlots) {
            if (!this.moveItemStackTo(stack, containerSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return itemStack;
    }

    public @NotNull ItemStack getInputStack() {
        return container.getItem(MaterialExtractorBlockEntity.INPUT_SLOT);
    }

    public @Nullable Material<?> getCurrentMaterial() {
        return MaterialExtractorBlockEntity.materialFromInput(getInputStack());
    }

    public @NotNull List<ItemStack> getCandidateStacks() {
        Material<?> material = getCurrentMaterial();
        if (material == null) return List.of();
        return MaterialExtractorBlockEntity.candidateItemStacks(material);
    }

    public boolean isSelectionRequired() {
        Material<?> material = getCurrentMaterial();
        return material != null && material.isTag();
    }

    public @Nullable String getCurrentMaterialKey() {
        Material<?> material = getCurrentMaterial();
        return material == null ? null : material.getName();
    }

    public @Nullable ResourceLocation getSelectedOutputId() {
        if (blockEntity == null) return null;
        return blockEntity.getSelectedOutputIdFor(getCurrentMaterialKey());
    }

    public @Nullable BlockPos getBlockPos() {
        return pos;
    }
}
