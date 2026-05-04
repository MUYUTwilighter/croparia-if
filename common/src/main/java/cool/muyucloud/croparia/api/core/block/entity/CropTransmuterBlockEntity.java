package cool.muyucloud.croparia.api.core.block.entity;

import cool.muyucloud.croparia.api.core.block.CropTransmuter;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.crop.AbstractFruit;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.repo.ContainerRepo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CropTransmuterBlockEntity extends BlockEntity implements MenuProvider, Container, WorldlyContainer, ExtendedMenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    private static final String NBT_SELECTED_INDEX = "SelectedIndex";
    private static final String NBT_POSITIVE_REDSTONE = "PositiveRedstone";

    private final NonNullList<ItemStack> inventory;
    private final ContainerRepo<CropTransmuterBlockEntity> repo = new ContainerRepo<>(this);
    private final RepoProxy<ItemSpec> inputProxy = RepoProxy.item(
        repo.lockConsume(INPUT_SLOT, OUTPUT_SLOT).lockAccept(OUTPUT_SLOT).trim()
    );
    private final RepoProxy<ItemSpec> outputProxy = RepoProxy.item(
        repo.lockAccept(INPUT_SLOT, OUTPUT_SLOT).lockConsume(INPUT_SLOT).trim()
    );
    private int selectedIndex = 0;
    private boolean positiveRedstone = true;

    public CropTransmuterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CROP_TRANSMUTER.get(), pos, state);
        this.inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CropTransmuterBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(CropTransmuter.POWERED) != powered) {
            level.setBlock(pos, state.setValue(CropTransmuter.POWERED, powered), 3);
        }
        if (powered != blockEntity.isPositiveRedstone()) return;
        blockEntity.tryProcess(serverLevel);
    }

    public void tryProcess(ServerLevel level) {
        ItemStack input = this.getItem(INPUT_SLOT);
        /* Get Output Stack */
        Optional<Material<?>> mayMaterial = this.readInputMaterial();
        if (mayMaterial.isEmpty()) return;
        List<ItemStack> candidates = mayMaterial.get().asItems();
        if (candidates.isEmpty()) {
            return;
        }
        ItemStack output = candidates.get(Math.min(candidates.size() - 1, this.getSelectedIndex()));
        /* Insert Output */
        ItemStack slot = this.getItem(OUTPUT_SLOT);
        if (slot.isEmpty()) {
            this.setItem(OUTPUT_SLOT, output.copy());
        } else if (ItemStack.isSameItemSameComponents(slot, output) && (slot.getMaxStackSize() - slot.getCount()) >= output.getCount()) {
            this.setItem(OUTPUT_SLOT, slot.copyWithCount(slot.getCount() + output.getCount()));
        } else {
            return;
        }
        input.shrink(1);
        this.setItem(INPUT_SLOT, input);
        this.setChanged();
    }

    public Optional<Material<?>> readInputMaterial() {
        Item input = this.getItem(INPUT_SLOT).getItem();
        if (input instanceof AbstractFruit<?> fruit) return Optional.of(fruit.getCrop().getMaterial());
        return Optional.empty();
    }

    public boolean setSelectedIndex(int selectedIndex) {
        if (selectedIndex < 0 || this.selectedIndex == selectedIndex) {
            return false;
        }
        this.selectedIndex = selectedIndex;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
        return true;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean isPositiveRedstone() {
        return positiveRedstone;
    }

    public boolean toggleRedstoneMode() {
        this.positiveRedstone = !this.positiveRedstone;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
        return this.positiveRedstone;
    }

    public @Nullable RepoProxy<ItemSpec> visitItem(@Nullable Direction direction) {
        return direction == Direction.DOWN ? outputProxy : inputProxy;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.inventory);
        this.selectedIndex = Math.max(0, input.getIntOr(NBT_SELECTED_INDEX, 0));
        this.positiveRedstone = input.getBooleanOr(NBT_POSITIVE_REDSTONE, true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, this.inventory);
        output.putInt(NBT_SELECTED_INDEX, this.selectedIndex);
        output.putBoolean(NBT_POSITIVE_REDSTONE, this.positiveRedstone);
        super.saveAdditional(output);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && stack.getItem() instanceof AbstractFruit<?>;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{OUTPUT_SLOT};
        }
        return new int[]{INPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && slot == INPUT_SLOT && stack.getItem() instanceof AbstractFruit<?>;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
            (double) this.worldPosition.getX() + 0.5,
            (double) this.worldPosition.getY() + 0.5,
            (double) this.worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public void clearContent() {
        if (this.inventory.isEmpty()) return;
        this.setChanged();
        this.inventory.clear();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Texts.translatable("container.croparia.crop_transmuter");
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new CropTransmuterMenu(syncId, inv, this);
    }
}
