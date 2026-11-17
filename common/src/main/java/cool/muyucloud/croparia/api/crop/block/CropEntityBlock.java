package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface CropEntityBlock<C extends AbstractCrop<?>> extends EntityBlock {
    BlockEntityType<CropEntity<C>> getBlockEntityType();

    @NotNull Identifier getId();

    default @NotNull ItemStack appendCropId(@NotNull LevelReader level, @NotNull BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof CropEntity<?> be) {
            be.getCropId().ifPresent(cropId -> stack.set(CropariaComponents.CROP_ID.get(), cropId));
        }
        return stack;
    }

    @Override
    default @Nullable BlockEntity newBlockEntity(@NotNull BlockPos worldPosition, @NotNull BlockState blockState) {
        return this.getBlockEntityType().create(worldPosition, blockState);
    }

    default @NotNull List<ItemStack> getCropDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder, Supplier<List<ItemStack>> defaultStacks) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Optional<Identifier> mayCropId = AbstractCrop.readCrop(be).map(AbstractCrop::getKey);
        if (mayCropId.isEmpty()) return defaultStacks.get();
        Identifier cropId = mayCropId.get();
        ReloadableServerRegistries.Holder registries = builder.getLevel().getServer().reloadableRegistries();
        Identifier id = this.getId();
        ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, CropariaIf.of(id.getPath() + "/" + cropId.getPath()));
        LootParams params = builder.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        return registries.getLootTable(lootKey).getRandomItems(params);
    }

    default void onCropEntityReady(@NotNull Level level, @NotNull CropEntity<C> entity) {
    }

    default MutableComponent getName(Level level, @NotNull BlockPos pos) {
        String key = level.getBlockState(pos).getBlock().getDescriptionId();
        return AbstractCrop.readCrop(level, pos).map(crop -> Texts.translatable(
            key,
            Texts.mayTranslatable(crop.getTranslationKey()).orElseGet(() -> Texts.literal(crop.translate()))
        )).orElseGet(() -> {
            if (this instanceof Block block) return block.getName();
            throw new UnsupportedOperationException("getName with context is not supported by non-block class without CropEntity");
        });
    }

    @SuppressWarnings("unchecked")
    static <C extends AbstractCrop<?>> Optional<CropEntityBlock<C>> tryGet(Level level, BlockPos pos) throws ClassCastException {
        return level.getBlockState(pos).getBlock() instanceof CropEntityBlock<?> ce ? Optional.of((CropEntityBlock<C>) ce) : Optional.empty();
    }
}
