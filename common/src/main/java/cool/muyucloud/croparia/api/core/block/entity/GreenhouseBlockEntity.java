package cool.muyucloud.croparia.api.core.block.entity;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.CropBlockAccess;
import cool.muyucloud.croparia.api.repo.ContainerRepo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.TagUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class GreenhouseBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static TagKey<Block> UNHARVESTABLE = TagKey.create(BuiltInRegistries.BLOCK.key(), CropariaIf.of("greenhouse_unharvestable"));

    private final NonNullList<ItemStack> inventory;
    private final RepoProxy<ItemSpec> proxy = RepoProxy.item(new ContainerRepo<>(this));

    public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.GREENHOUSE_BE.get(), pos, state);
        this.inventory = NonNullList.withSize(9, ItemStack.EMPTY);
    }

    public void tryHarvest() {
        if (!(this.getLevel() instanceof ServerLevel sLevel)) return;
        BlockPos gPos = this.getBlockPos();
        BlockPos cPos = gPos.below();
        BlockState crop = sLevel.getBlockState(cPos);
        Block belowBlock = crop.getBlock();
        if (TagUtil.isIn(UNHARVESTABLE, belowBlock)) return;
        if (belowBlock instanceof AttachedStemBlock) {
            this.tryHarvestMelon(sLevel, crop, cPos);
        } else if (belowBlock instanceof CropBlock) {
            this.tryHarvestCrop(sLevel, crop, cPos);
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
        // Seed reduction
        for (ItemStack stack : droppedStacks) {
            if (stack.is(seed)) { // Consume one seed to simulate "replant"
                stack.shrink(1);    // No worry if aborted, the stack is newly created, so no side effect
                break;
            }
        }
        // Deposit
        if (!this.tryDeposit(droppedStacks)) return;    // Not enough space, abort
        // Age update
        IntegerProperty property = CropBlockAccess.of(cropBlock).cif$getAgeProperty();
        int maxAge = cropBlock.getMaxAge();
        if (maxAge == 0) {
            level.destroyBlock(worldPosition.below(), false);
        } else {
            level.setBlockAndUpdate(worldPosition.below(), cropBlock.defaultBlockState().setValue(property, maxAge / 2));
        }
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean tryDeposit(List<ItemStack> droppedStacks) {
        if (droppedStacks.isEmpty()) return true;
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
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.inventory);
        this.tryHarvest();
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        ContainerHelper.saveAllItems(output, this.inventory);
        super.saveAdditional(output);
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        this.tryHarvest();
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        ItemStack stored = this.getItem(slot);
        if (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() == stack.getCount()) {
            return;
        }
        this.setChanged();
        this.inventory.set(slot, stack);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
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
    public AbstractContainerMenu createMenu(int syncId, @NonNull Inventory inv, @NonNull Player player) {
        return new DispenserMenu(syncId, inv, this);
    }

    public @Nullable RepoProxy<ItemSpec> visitItem() {
        return proxy;
    }
}
