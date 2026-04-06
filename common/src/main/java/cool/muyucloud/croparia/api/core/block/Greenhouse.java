//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cool.muyucloud.croparia.api.core.block;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.core.block.entity.GreenhouseBlockEntity;
import cool.muyucloud.croparia.api.repo.ProxyProvider;
import cool.muyucloud.croparia.registry.CropariaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Greenhouse extends BaseEntityBlock {
    public static final MapCodec<Greenhouse> CODEC = simpleCodec(Greenhouse::new);
    public static TagKey<Block> UNHARVESTABLE = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.tryParse("croparia:greenhouse_unharvestable"));
    protected final VoxelShape SHAPE = Block.box(1.0, 1.0, 0.0, 15.0, 3.0, 15.0);

    public Greenhouse(Properties settings) {
        super(settings);
        ProxyProvider.registerItem((world, pos, state, be, direction) -> {
            if (be instanceof GreenhouseBlockEntity gbe) {
                return gbe.visitItem();
            } else {
                return null;
            }
        }, this);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.updateShape(state, Direction.DOWN, state, level, pos, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        // Filter client level
        if (!(level instanceof ServerLevel serverLevel)) return state;
        // Filter unharvestable blocks
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        // Filter block entity
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GreenhouseBlockEntity gbe)) return state;
        // Do harvest
        gbe.tryHarvest(serverLevel, belowState, belowPos);
        return state;
    }

    @Override
    public void randomTick(@Nullable BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getBlockState(pos.below()).getBlock() instanceof CropBlock) {
            world.getBlockState(pos.below()).randomTick(world, pos.below(), random);
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!world.isClientSide) {
            MenuProvider screenHandlerFactory = state.getMenuProvider(world, pos);
            if (screenHandlerFactory != null) {
                player.openMenu(screenHandlerFactory);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof GreenhouseBlockEntity) {
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
        return this.SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GreenhouseBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull Item asItem() {
        return CropariaItems.GREENHOUSE.get();
    }
}
