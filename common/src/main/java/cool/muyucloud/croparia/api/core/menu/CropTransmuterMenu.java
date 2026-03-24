package cool.muyucloud.croparia.api.core.menu;

import cool.muyucloud.croparia.api.core.block.entity.CropTransmuterBlockEntity;
import cool.muyucloud.croparia.registry.MenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class CropTransmuterMenu extends AbstractContainerMenu {
    private final CropTransmuterBlockEntity blockEntity;
    private final BlockPos blockPos;
    private int positiveRedstone = 1;

    public CropTransmuterMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, resolve(inventory, buf));
    }

    private CropTransmuterMenu(int syncId, Inventory inventory, Resolved resolved) {
        this(syncId, inventory, resolved.blockPos(), resolved.blockEntity());
    }

    public CropTransmuterMenu(int syncId, Inventory inventory, BlockEntity blockEntity) {
        this(syncId, inventory, blockEntity.getBlockPos(), blockEntity);
    }

    public CropTransmuterMenu(int syncId, Inventory inventory, BlockPos blockPos, BlockEntity blockEntity) {
        super(MenuTypes.CROP_TRANSMUTER.get(), syncId);
        if (!(blockEntity instanceof CropTransmuterBlockEntity transmuter)) throw new IllegalArgumentException("Tring to open Crop Transmuter over an incorrect block!");
        this.blockEntity = transmuter;
        this.blockPos = blockPos.immutable();
        this.positiveRedstone = transmuter.isPositiveRedstone() ? 1 : 0;
        checkContainerSize(this.getBlockEntity(), CropTransmuterBlockEntity.INVENTORY_SIZE);
        this.getBlockEntity().startOpen(inventory.player);
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return CropTransmuterMenu.this.getBlockEntity().isPositiveRedstone() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                CropTransmuterMenu.this.positiveRedstone = value;
            }
        });
        this.addSlot(new Slot(this.getBlockEntity(), CropTransmuterBlockEntity.INPUT_SLOT, 20, 35));
        this.addSlot(new Slot(this.getBlockEntity(), CropTransmuterBlockEntity.OUTPUT_SLOT, 138, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public CropTransmuterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean isPositiveRedstone() {
        return positiveRedstone != 0;
    }

    private static Resolved resolve(Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new Resolved(pos, inventory.player.level().getBlockEntity(pos));
    }

    private record Resolved(BlockPos blockPos, BlockEntity blockEntity) {}

    @Override
    public boolean stillValid(Player player) {
        return this.getBlockEntity().stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.getBlockEntity().stopOpen(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
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
}
