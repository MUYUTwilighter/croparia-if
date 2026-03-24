package cool.muyucloud.croparia.api.core.block.entity;

import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.ItemMaterial;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.core.block.CropTransmuter;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.WorldlyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CropTransmuterBlockEntity extends BlockEntity implements MenuProvider, Container, WorldlyContainer, ExtendedMenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;

    private static final String NBT_SELECTED_INDEX = "SelectedIndex";
    private static final String NBT_POSITIVE_REDSTONE = "PositiveRedstone";

    private final NonNullList<ItemStack> inventory;
    private final RepoProxy<ItemSpec> proxy = RepoProxy.item(new ExtractorRepo(this));
    private int selectedIndex = 0;
    private boolean positiveRedstone = true;

    public CropTransmuterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CROP_TRANSMUTER.get(), pos, state);
        this.inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CropTransmuterBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!blockEntity.shouldProcess(state)) return;
        blockEntity.tryProcess(serverLevel);
    }

    public void tryProcess(ServerLevel level) {
        ItemStack input = this.getItem(INPUT_SLOT);
        if (input.isEmpty()) return;
        Material<?> material = materialFromInput(input);
        if (material == null) return;
        ItemStack output = resolveOutput(material);
        if (output.isEmpty()) return;
        if (!insertIntoOutput(output.copy())) return;
        input.shrink(1);
        this.setItem(INPUT_SLOT, input);
        this.setChanged();
    }

    public static @Nullable Material<?> materialFromInput(ItemStack stack) {
        AbstractCrop<?> crop = cropFromInput(stack);
        if (crop == null) return null;
        return crop.getMaterial();
    }

    public static @Nullable AbstractCrop<?> cropFromInput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof CropAccess<?> access) {
            return access.getCrop();
        }
        return null;
    }

    public static @NotNull List<ItemStack> candidateItemStacks(Material<?> material) {
        if (material instanceof ItemMaterial itemMaterial) {
            List<ItemStack> result = new ArrayList<>();
            for (Item item : itemMaterial.candidates()) {
                ItemStack stack = item.getDefaultInstance();
                stack.applyComponents(itemMaterial.getComponents());
                stack.setCount(Math.min(stack.getMaxStackSize(), itemMaterial.getCount()));
                result.add(stack);
            }
            return result;
        }
        if (material instanceof BlockMaterial blockMaterial) {
            List<ItemStack> result = new ArrayList<>();
            for (Block block : blockMaterial.candidates()) {
                Item item = block.asItem();
                if (item == Items.AIR) continue;
                ItemStack stack = item.getDefaultInstance();
                stack.setCount(Math.min(stack.getMaxStackSize(), blockMaterial.getCount()));
                result.add(stack);
            }
            return result;
        }
        return List.of();
    }

    public static @NotNull Set<ResourceLocation> candidateItemIds(Material<?> material) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (ItemStack stack : candidateItemStacks(material)) {
            ResourceLocation id = stack.getItem().arch$registryName();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private ItemStack resolveOutput(Material<?> material) {
        List<ItemStack> candidates = candidateItemStacks(material);
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int clampedIndex = getSelectedIndexFor(material);
        if (clampedIndex < 0 || clampedIndex >= candidates.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack candidate = candidates.get(clampedIndex);
        Item selected = candidate.getItem();
        if (material instanceof ItemMaterial itemMaterial) {
            ItemStack stack = selected.getDefaultInstance();
            stack.applyComponents(itemMaterial.getComponents());
            stack.setCount(Math.min(stack.getMaxStackSize(), itemMaterial.getCount()));
            return stack;
        }
        if (material instanceof BlockMaterial blockMaterial) {
            ItemStack stack = selected.getDefaultInstance();
            stack.setCount(Math.min(stack.getMaxStackSize(), blockMaterial.getCount()));
            return stack;
        }
        return ItemStack.EMPTY;
    }

    private boolean insertIntoOutput(ItemStack stack) {
        ItemStack stored = this.getItem(OUTPUT_SLOT);
        if (stored.isEmpty()) {
            this.setItem(OUTPUT_SLOT, stack.copy());
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(stored, stack)) {
            return false;
        }
        int max = Math.min(stored.getMaxStackSize(), this.getMaxStackSize());
        int room = max - stored.getCount();
        if (room <= 0) return false;
        if (room < stack.getCount()) {
            return false;
        }
        stored.grow(stack.getCount());
        this.setItem(OUTPUT_SLOT, stored);
        return true;
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

    public int getSelectedIndexFor(@Nullable Material<?> material) {
        if (material == null) {
            return selectedIndex;
        }
        int size = candidateItemStacks(material).size();
        if (size <= 0) {
            return 0;
        }
        return Math.min(selectedIndex, size - 1);
    }

    public @Nullable ResourceLocation getSelectedOutputIdFor(@Nullable Material<?> material) {
        if (material == null) return null;
        List<ItemStack> candidates = candidateItemStacks(material);
        if (candidates.isEmpty()) return null;
        ResourceLocation id = candidates.get(getSelectedIndexFor(material)).getItem().arch$registryName();
        return id;
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

    public @Nullable RepoProxy<ItemSpec> visitItem() {
        return proxy;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, this.inventory, provider);
        this.selectedIndex = Math.max(0, nbt.getInt(NBT_SELECTED_INDEX));
        this.positiveRedstone = !nbt.contains(NBT_POSITIVE_REDSTONE) || nbt.getBoolean(NBT_POSITIVE_REDSTONE);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        ContainerHelper.saveAllItems(nbt, this.inventory, provider);
        nbt.putInt(NBT_SELECTED_INDEX, this.selectedIndex);
        nbt.putBoolean(NBT_POSITIVE_REDSTONE, this.positiveRedstone);
        super.saveAdditional(nbt, provider);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        refreshSelection(false);
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
        if (slot == INPUT_SLOT) {
            refreshSelection(true);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != INPUT_SLOT) return false;
        return cropFromInput(stack) != null;
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
        if (direction == Direction.DOWN) return false;
        return slot == INPUT_SLOT && cropFromInput(stack) != null;
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

    private void refreshSelection(boolean notify) {
        Material<?> material = materialFromInput(getItem(INPUT_SLOT));
        if (material == null || candidateItemStacks(material).isEmpty()) {
            return;
        }
        int clamped = getSelectedIndexFor(material);
        if (clamped != this.selectedIndex) {
            this.selectedIndex = clamped;
            if (notify) {
                this.setChanged();
                if (this.level != null) {
                    this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
                }
            }
        }
    }

    private boolean shouldProcess(BlockState state) {
        boolean powered = state.getValue(CropTransmuter.POWERED);
        return positiveRedstone ? powered : !powered;
    }

    private static final class ExtractorRepo implements Repo<ItemSpec> {
        private final CropTransmuterBlockEntity entity;

        private ExtractorRepo(CropTransmuterBlockEntity entity) {
            this.entity = entity;
        }

        @Override
        public int size() {
            return entity.getContainerSize();
        }

        @Override
        public TypeToken<ItemSpec> getType() {
            return ItemSpec.TYPE;
        }

        @Override
        public boolean isEmpty(int i) {
            return entity.getItem(i).isEmpty();
        }

        @Override
        public ItemSpec resourceFor(int i) {
            return ItemSpec.of(entity.getItem(i));
        }

        @Override
        public long simConsume(int i, ItemSpec resource, long amount) {
            if (i != OUTPUT_SLOT) return 0;
            ItemStack stack = entity.getItem(i);
            if (!resource.is(stack)) return 0;
            return Math.min(amount, stack.getCount());
        }

        @Override
        public long consume(int i, ItemSpec resource, long amount) {
            if (i != OUTPUT_SLOT) return 0;
            ItemStack stack = entity.getItem(i);
            if (!resource.is(stack)) return 0;
            int stored = stack.getCount();
            int consumed = CifUtil.toIntSafe(Math.min(amount, stored));
            stack.shrink(consumed);
            entity.setItem(i, stack);
            return consumed;
        }

        @Override
        public long simAccept(int i, ItemSpec resource, long amount) {
            if (i != INPUT_SLOT) return 0;
            if (!(resource.getResource() instanceof CropAccess<?>)) return 0;
            ItemStack stored = entity.getItem(i);
            if (!stored.isEmpty() && !resource.is(stored)) return 0;
            long capacity = capacityFor(i, resource);
            long room = capacity - amountFor(i);
            return Math.min(amount, Math.max(room, 0));
        }

        @Override
        public long accept(int i, ItemSpec resource, long amount) {
            if (i != INPUT_SLOT) return 0;
            if (!(resource.getResource() instanceof CropAccess<?>)) return 0;
            ItemStack stored = entity.getItem(i);
            if (!stored.isEmpty() && !resource.is(stored)) return 0;
            long capacity = capacityFor(i, resource);
            long room = capacity - amountFor(i);
            long accepted = Math.min(amount, Math.max(room, 0));
            if (accepted <= 0) return 0;
            int total = CifUtil.toIntSafe(accepted + stored.getCount());
            entity.setItem(i, resource.createStack(total));
            return accepted;
        }

        @Override
        public long capacityFor(int i, ItemSpec resource) {
            if (i != INPUT_SLOT) return 0;
            if (!(resource.getResource() instanceof CropAccess<?>)) return 0;
            ItemStack stored = entity.getItem(i);
            ItemStack toPlace = resource.createStack();
            int containerSize = entity.getMaxStackSize(toPlace);
            if (stored.isEmpty() || resource.is(stored)) {
                return containerSize;
            }
            return 0;
        }

        @Override
        public long amountFor(int i, ItemSpec resource) {
            ItemStack stored = entity.getItem(i);
            if (resource.is(stored)) {
                return stored.getCount();
            }
            return 0;
        }

        @Override
        public long amountFor(int i) {
            return entity.getItem(i).getCount();
        }
    }
}
