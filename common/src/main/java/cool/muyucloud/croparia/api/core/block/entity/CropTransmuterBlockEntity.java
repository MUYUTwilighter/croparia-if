package cool.muyucloud.croparia.api.core.block.entity;

import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.ItemMaterial;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.core.menu.MaterialExtractorMenu;
import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MaterialExtractorBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_COUNT = 9;
    public static final int INVENTORY_SIZE = OUTPUT_START + OUTPUT_COUNT;

    private static final String NBT_SELECTED_MATERIAL = "SelectedMaterial";
    private static final String NBT_SELECTED_OUTPUT = "SelectedOutput";

    private final NonNullList<ItemStack> inventory;
    private final RepoProxy<ItemSpec> proxy = RepoProxy.item(new ExtractorRepo(this));
    private @Nullable String selectedMaterialKey = null;
    private @Nullable ResourceLocation selectedOutputId = null;

    public MaterialExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.MATERIAL_EXTRACTOR.get(), pos, state);
        this.inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MaterialExtractorBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.hasNeighborSignal(pos)) return;
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
        if (!material.isTag()) {
            return material.asItem();
        }
        String materialKey = material.getName();
        if (!Objects.equals(materialKey, this.selectedMaterialKey) || this.selectedOutputId == null) {
            return ItemStack.EMPTY;
        }
        Set<ResourceLocation> candidates = candidateItemIds(material);
        if (!candidates.contains(this.selectedOutputId)) {
            return ItemStack.EMPTY;
        }
        Item selected = BuiltInRegistries.ITEM.get(this.selectedOutputId);
        if (selected == Items.AIR) {
            return ItemStack.EMPTY;
        }
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
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack stored = this.getItem(i);
            if (stored.isEmpty()) {
                this.setItem(i, stack.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(stored, stack)) {
                int max = Math.min(stored.getMaxStackSize(), this.getMaxStackSize());
                int room = max - stored.getCount();
                if (room <= 0) continue;
                int toMove = Math.min(room, stack.getCount());
                stored.grow(toMove);
                stack.shrink(toMove);
                this.setItem(i, stored);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        return stack.isEmpty();
    }

    public boolean setSelectedOutput(@NotNull String materialKey, @NotNull ResourceLocation outputId) {
        if (Objects.equals(this.selectedMaterialKey, materialKey) && Objects.equals(this.selectedOutputId, outputId)) {
            return false;
        }
        this.selectedMaterialKey = materialKey;
        this.selectedOutputId = outputId;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
        return true;
    }

    public @Nullable ResourceLocation getSelectedOutputIdFor(@Nullable String materialKey) {
        if (materialKey == null) return null;
        if (!Objects.equals(materialKey, this.selectedMaterialKey)) return null;
        return this.selectedOutputId;
    }

    public @Nullable String getSelectedMaterialKey() {
        return selectedMaterialKey;
    }

    public @Nullable ResourceLocation getSelectedOutputId() {
        return selectedOutputId;
    }

    public @Nullable RepoProxy<ItemSpec> visitItem() {
        return proxy;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, this.inventory, provider);
        if (nbt.contains(NBT_SELECTED_MATERIAL)) {
            String key = nbt.getString(NBT_SELECTED_MATERIAL);
            this.selectedMaterialKey = key.isEmpty() ? null : key;
        } else {
            this.selectedMaterialKey = null;
        }
        if (nbt.contains(NBT_SELECTED_OUTPUT)) {
            String id = nbt.getString(NBT_SELECTED_OUTPUT);
            this.selectedOutputId = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        } else {
            this.selectedOutputId = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        ContainerHelper.saveAllItems(nbt, this.inventory, provider);
        if (this.selectedMaterialKey != null) {
            nbt.putString(NBT_SELECTED_MATERIAL, this.selectedMaterialKey);
        }
        if (this.selectedOutputId != null) {
            nbt.putString(NBT_SELECTED_OUTPUT, this.selectedOutputId.toString());
        }
        super.saveAdditional(nbt, provider);
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
        if (slot != INPUT_SLOT) return false;
        return cropFromInput(stack) != null;
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
        return Texts.translatable("container.croparia.material_extractor");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new MaterialExtractorMenu(syncId, inv, this);
    }

    private static final class ExtractorRepo implements Repo<ItemSpec> {
        private final MaterialExtractorBlockEntity entity;

        private ExtractorRepo(MaterialExtractorBlockEntity entity) {
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
            if (i == INPUT_SLOT) return 0;
            ItemStack stack = entity.getItem(i);
            if (!resource.is(stack)) return 0;
            return Math.min(amount, stack.getCount());
        }

        @Override
        public long consume(int i, ItemSpec resource, long amount) {
            if (i == INPUT_SLOT) return 0;
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
