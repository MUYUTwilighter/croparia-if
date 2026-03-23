package cool.muyucloud.croparia.api.core.block;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.core.block.entity.MaterialExtractorBlockEntity;
import cool.muyucloud.croparia.api.repo.ProxyProvider;
import cool.muyucloud.croparia.registry.BlockEntities;
import cool.muyucloud.croparia.registry.CropariaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaterialExtractor extends BaseEntityBlock {
    public static final MapCodec<MaterialExtractor> CODEC = simpleCodec(MaterialExtractor::new);
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public MaterialExtractor(Properties settings) {
        super(settings);
        ProxyProvider.registerItem((world, pos, state, be, direction) -> {
            if (be instanceof MaterialExtractorBlockEntity extractor) {
                return extractor.visitItem();
            }
            return null;
        }, this);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        InteractionHand interactionHand,
        BlockHitResult blockHitResult
    ) {
        if (!world.isClientSide) {
            MenuProvider menu = state.getMenuProvider(world, pos);
            if (menu != null) {
                player.openMenu(menu);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MaterialExtractorBlockEntity) {
                Containers.dropContentsOnDestroy(state, newState, world, pos);
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterialExtractorBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull Item asItem() {
        return CropariaItems.MATERIAL_EXTRACTOR.get();
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper(
            blockEntityType,
            BlockEntities.MATERIAL_EXTRACTOR.get(),
            MaterialExtractorBlockEntity::serverTick
        );
    }
}
