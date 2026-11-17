package cool.muyucloud.croparia.api.crop.block.entity;

import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.block.CropEntityBlock;
import cool.muyucloud.croparia.registry.CropariaComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CropEntity<C extends AbstractCrop<?>> extends BlockEntity {
    private final @NotNull CropRegistry<? super C> registry;
    private boolean pendingSync = false;

    public CropEntity(BlockEntityType<CropEntity<C>> type, BlockPos worldPosition, BlockState blockState, @NotNull CropRegistry<? super C> registry) {
        super(type, worldPosition, blockState);
        this.registry = registry;
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        if (level.getServer() == null) return;
        level.getServer().execute(() -> {
            if (this.isRemoved()) return;
            CropEntityBlock.<C>tryGet(level, this.getBlockPos()).ifPresent(ceBlock -> ceBlock.onCropEntityReady(level, this));
        });
    }

    public @NotNull CropRegistry<? super C> getRegistry() {
        return registry;
    }

    protected boolean getPendingSync() {
        return pendingSync;
    }

    public Optional<Identifier> getCropId() {
        return Optional.ofNullable(this.components().get(CropariaComponents.CROP_ID.get()));
    }

    public void setCropId(Identifier id) {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.addAll(this.components()).set(CropariaComponents.CROP_ID.get(), id);
        DataComponentMap newMap = builder.build();
        this.applyImplicitComponents(newMap);
        this.setComponents(newMap);
        this.setChanged();
    }

    @SuppressWarnings("unchecked")
    public Optional<C> getCrop() {
        return (Optional<C>) this.getCropId().flatMap(id -> this.getRegistry().getById(id));
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentGetter components) {
        super.applyImplicitComponents(components);
        // CROP_ID stays in the component map; reading it here would consume and remove it.
        this.pendingSync = true;
    }

    /**
     * Define what BE data to sync.
     *
     * @see #getUpdatePacket()
     */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.getCropId().ifPresent(id -> output.store("components", DataComponentMap.CODEC,
            DataComponentMap.builder().set(CropariaComponents.CROP_ID.get(), id).build()));
        return output.buildResult();
    }

    /**
     * Enable BE data sync.
     *
     * @see #getUpdateTag(HolderLookup.Provider)
     */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        if (this.getCropId().isPresent()) {
            return ClientboundBlockEntityDataPacket.create(this);
        }
        return null;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        Level level = this.getLevel();
        if (level == null) return;
        if (!level.isClientSide() && this.getPendingSync()) {
            this.pendingSync = false;
            CropEntityBlock.<C>tryGet(level, this.getBlockPos()).ifPresent(block -> block.onCropEntityReady(level, this));
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
