//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cool.muyucloud.croparia.api.core.block.entity;

import cool.muyucloud.croparia.access.CropBlockAccess;
import cool.muyucloud.croparia.api.repo.ContainerRepo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreenhouseBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> inventory;
    private final RepoProxy<ItemSpec> proxy = RepoProxy.item(new ContainerRepo(this));

    public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GREENHOUSE_BE.get(), pos, state);
        this.inventory = NonNullList.withSize(9, ItemStack.EMPTY);
    }

    public void tryHarvest(ServerLevel level, BlockState crop, BlockPos cropPos) {
        Block belowBlock = crop.getBlock();
        if (belowBlock instanceof AttachedStemBlock) {
            tryHarvestMelon(level, crop, cropPos);
        } else if (belowBlock instanceof CropBlock) {
            tryHarvestCrop(level, crop, cropPos);
        }
    }

    /**
     * @param level   The current level
     * @param crop    BlockState of the crop block, must be an instance of {@link CropBlock}
     * @param cropPos Position of the crop block
     */
    public void tryHarvestCrop(ServerLevel level, BlockState crop, BlockPos cropPos) {
        CropBlock cropBlock = (CropBlock) crop.getBlock();
        // Age check
        if (!cropBlock.isMaxAge(crop)) return;
        Item seed = cropBlock.asItem();
        List<ItemStack> droppedStacks = Block.getDrops(crop, level, cropPos, level.getBlockEntity(cropPos));
        boolean decreased = false;
        // Seed reduction
        for (ItemStack stack : droppedStacks) {
            if (!decreased && stack.is(seed)) { // Consume one seed to simulate "replant"
                stack.shrink(1);    // No worry if aborted, the stack is newly created, so no side effect
                decreased = true;
            }
        }
        // Deposit
        if (!this.tryDeposit(droppedStacks)) return;    // Not enough space, abort
        // Age update
        IntegerProperty property = CropBlockAccess.of(cropBlock).cif$getAgeProperty();
        int maxAge = cropBlock.getMaxAge();
        level.setBlockAndUpdate(worldPosition.below(), cropBlock.defaultBlockState().setValue(property, maxAge / 2));
    }

    public void tryHarvestMelon(ServerLevel level, BlockState stem, BlockPos stemPos) {
        // Find melon
        Direction facing = stem.getValue(AttachedStemBlock.FACING);
        BlockPos melonPos = stemPos.offset(facing.getUnitVec3i());
        BlockState melonState = level.getBlockState(melonPos);
        // Get drops
        List<ItemStack> droppedStacks = Block.getDrops(melonState, level, melonPos, level.getBlockEntity(melonPos));
        // Deposit
        if (!this.tryDeposit(droppedStacks)) return;    // Not enough space, abort
        // Remove melon
        level.removeBlock(melonPos, false);
    }

    public boolean tryDeposit(List<ItemStack> droppedStacks) {
        boolean doAccept = false;
        for (ItemStack stack : droppedStacks) {
            if (!stack.isEmpty()) this.setChanged();
            long accepted = this.proxy.accept(ItemSpec.of(stack), stack.getCount());
            if (!doAccept && accepted >= 0) {   // Not enough space, abort
                doAccept = true;
            }
        }
        return doAccept;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, this.inventory, provider);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        ContainerHelper.saveAllItems(nbt, this.inventory, provider);
        super.saveAdditional(nbt, provider);
    }

    public int getContainerSize() {
        return this.inventory.size();
    }

    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    public @NotNull ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack stored = this.getItem(slot);
        if (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() == stack.getCount()) {
            return;
        }
        this.setChanged();
        this.inventory.set(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5, (double) this.worldPosition.getY() + 0.5, (double) this.worldPosition.getZ() + 0.5) <= 64.0;
        }
    }

    @Override
    public void clearContent() {
        if (this.inventory.isEmpty()) return;
        this.setChanged();
        this.inventory.clear();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Texts.translatable("container.croparia.greenhouse");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new DispenserMenu(syncId, inv, this);
    }

    public @Nullable RepoProxy<ItemSpec> visitItem() {
        return proxy;
    }
}
