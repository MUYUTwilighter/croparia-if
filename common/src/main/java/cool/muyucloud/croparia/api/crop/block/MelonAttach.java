package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.block.entity.CropEntity;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

public class MelonAttach extends AttachedStemBlock implements CropEntityBlock<Melon> {
    @SuppressWarnings("unchecked")
    public MelonAttach(ResourceKey<? extends Block> stem, ResourceKey<? extends Block> melon, ResourceKey<? extends Item> seed, TagKey<Block> stemBase, Properties properties) {
        super((ResourceKey<Block>) stem, (ResourceKey<Block>) melon, (ResourceKey<Item>) seed, stemBase, properties);
    }

    @Override
    public @NotNull Item asItem() {
        return CropariaItems.MELON_SEED.get();
    }

    @Override
    public BlockEntityType<CropEntity<Melon>> getBlockEntityType() {
        return BlockEntities.MELON.get();
    }

    @Override
    public @NotNull Identifier getId() {
        return Objects.requireNonNull(this.arch$registryName());
    }

    @Override
    protected @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        return this.appendCropId(level, pos, stack);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        return this.getCropDrops(state, builder, () -> super.getDrops(state, builder));
    }

    @Override
    protected boolean shouldChangedStateKeepBlockEntity(@NotNull BlockState oldState) {
        return this.getBlockEntityType().isValid(oldState);
    }

    @Override
    public @NonNull MutableComponent getName() {
        return Texts.translatable("block.croparia.melon_attach", Texts.translatable("melon.croparia.unknown"));
    }
}
