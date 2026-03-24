package cool.muyucloud.croparia.api.core.menu;

import cool.muyucloud.croparia.api.core.block.entity.CropTransmuterBlockEntity;
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

public class CropTransmuterMenu extends AbstractContainerMenu {
    private final Container container;
    private final @Nullable CropTransmuterBlockEntity blockEntity;
    private final BlockPos pos;

    public CropTransmuterMenu(int syncId, Inventory inventory, CropTransmuterBlockEntity blockEntity) {
        this(syncId, inventory, blockEntity, blockEntity.getBlockPos());
    }

    public CropTransmuterMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, resolveBlockEntity(inventory, buf));
    }

    private CropTransmuterMenu(int syncId, Inventory inventory, Resolved resolved) {
        this(syncId, inventory, resolved.blockEntity, resolved.pos);
    }

    private CropTransmuterMenu(int syncId, Inventory inventory, @Nullable CropTransmuterBlockEntity blockEntity, BlockPos pos) {
        super(MenuTypes.CROP_TRANSMUTER.get(), syncId);
        this.blockEntity = blockEntity;
        this.pos = pos;
        this.container = blockEntity == null ? new SimpleContainer(CropTransmuterBlockEntity.INVENTORY_SIZE) : blockEntity;
        checkContainerSize(this.container, CropTransmuterBlockEntity.INVENTORY_SIZE);
        this.container.startOpen(inventory.player);
        addSlots(inventory);
    }

    private static Resolved resolveBlockEntity(Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof CropTransmuterBlockEntity extractor) {
            return new Resolved(extractor, pos);
        }
        return new Resolved(null, pos);
    }

    private record Resolved(@Nullable CropTransmuterBlockEntity blockEntity, BlockPos pos) {}

    private void addSlots(Inventory playerInventory) {
        this.addSlot(new Slot(container, CropTransmuterBlockEntity.INPUT_SLOT, 20, 35));
        this.addSlot(new Slot(container, CropTransmuterBlockEntity.OUTPUT_SLOT, 138, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
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
        int containerSlots = CropTransmuterBlockEntity.INVENTORY_SIZE;
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
        return container.getItem(CropTransmuterBlockEntity.INPUT_SLOT);
    }

    public @Nullable Material<?> getCurrentMaterial() {
        return CropTransmuterBlockEntity.materialFromInput(getInputStack());
    }

    public @NotNull List<ItemStack> getCandidateStacks() {
        Material<?> material = getCurrentMaterial();
        if (material == null) return List.of();
        return CropTransmuterBlockEntity.candidateItemStacks(material);
    }

    public boolean hasMaterial() {
        return getCurrentMaterial() != null;
    }

    public @Nullable String getCurrentMaterialKey() {
        Material<?> material = getCurrentMaterial();
        return material == null ? null : material.getName();
    }

    public @Nullable ResourceLocation getSelectedOutputId() {
        if (blockEntity == null) return null;
        return blockEntity.getSelectedOutputIdFor(getCurrentMaterial());
    }

    public @Nullable BlockPos getBlockPos() {
        return pos;
    }

    public int getSelectedIndex() {
        if (blockEntity == null) return 0;
        return blockEntity.getSelectedIndexFor(getCurrentMaterial());
    }

    public boolean isPositiveRedstone() {
        return blockEntity == null || blockEntity.isPositiveRedstone();
    }
}
