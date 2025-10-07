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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class GreenhouseBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> inventory;
    private final RepoProxy<ItemSpec> proxy = RepoProxy.item(new ContainerRepo(this));

    public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GREENHOUSE_BE.get(), pos, state);
        this.inventory = NonNullList.withSize(9, ItemStack.EMPTY);
    }

    public static void tick(Level level, BlockPos worldPosition, GreenhouseBlockEntity gbe) {
        if (!level.isClientSide) {
            BlockState belowState = level.getBlockState(worldPosition.below());
            if (belowState.getBlock() instanceof CropBlock block) {
                Item seed = block.asItem();
                if (block.isMaxAge(belowState)) {
                    List<ItemStack> droppedStacks = Block.getDrops(belowState, Objects.requireNonNull(level.getServer()).getLevel(level.dimension()), worldPosition.below(), level.getBlockEntity(worldPosition.below()));
                    boolean decreased = false;
                    for (ItemStack stack : droppedStacks) {
                        if (!decreased && stack.is(seed)) { // We consume one seed to simulate "replant"
                            stack.shrink(1);    // No worry if aborted, the stack is newly created, so no side effect
                            decreased = true;
                        }
                        long sim = gbe.proxy.simAccept(ItemSpec.of(stack), stack.getCount());
                        if (sim < stack.getCount()) {   // Not enough space, abort
                            return;
                        }
                    }
                    for (ItemStack stack : droppedStacks) {
                        if (!stack.isEmpty()) gbe.setChanged();
                        gbe.proxy.accept(ItemSpec.of(stack), stack.getCount());
                    }
                    IntegerProperty property = ((CropBlockAccess) block).cif$getAgeProperty();
                    int maxAge = block.getMaxAge();
                    level.setBlockAndUpdate(worldPosition.below(), block.defaultBlockState().setValue(property, maxAge / 2));
                }
            }
        }
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

    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    public void setItem(int slot, ItemStack stack) {
        ItemStack stored = this.getItem(slot);
        if (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() == stack.getCount()) {
            return;
        }
        this.setChanged();
        this.inventory.set(slot, stack);
    }

    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5, (double) this.worldPosition.getY() + 0.5, (double) this.worldPosition.getZ() + 0.5) <= 64.0;
        }
    }

    public void clearContent() {
        if (this.inventory.isEmpty()) return;
        this.setChanged();
        this.inventory.clear();
    }

    public @NotNull Component getDisplayName() {
        return Component.nullToEmpty("Greenhouse");
    }

    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new DispenserMenu(syncId, inv, this);
    }

    public @Nullable RepoProxy<ItemSpec> visitItem() {
        return proxy;
    }
}
