
package cool.muyucloud.croparia.api.core.block;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.core.block.entity.GreenhouseBlockEntity;
import cool.muyucloud.croparia.api.repo.ProxyProvider;
import cool.muyucloud.croparia.registry.CropariaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class Greenhouse extends BaseEntityBlock {
    public static final MapCodec<Greenhouse> CODEC = simpleCodec(Greenhouse::new);
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

    public static void tryHarvest(Level level, BlockPos gPos) {
        BlockEntity be = level.getBlockEntity(gPos);
        if (!(be instanceof GreenhouseBlockEntity gbe)) return;
        gbe.tryHarvest();
    }

    @Override
    protected void onPlace(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        tryHarvest(level, pos);
    }

    @Override
    protected @NonNull BlockState updateShape(
        @NonNull BlockState state, @NonNull LevelReader level, @NonNull ScheduledTickAccess scheduledTickAccess,
        @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos,
        @NonNull BlockState neighborState, @NonNull RandomSource random
    ) {
        super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        if (level instanceof Level iLevel) {
            tryHarvest(iLevel, pos);
        }
        return level.getBlockState(pos);
    }

    @Override
    protected void neighborChanged(
        @NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Block neighborBlock,
        @Nullable Orientation orientation, boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        tryHarvest(level, pos);
    }

    @Override
    public void randomTick(@Nullable BlockState state, ServerLevel world, BlockPos pos, @NonNull RandomSource random) {
        world.getBlockState(pos.below()).randomTick(world, pos.below(), random);
        tryHarvest(world, pos);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(
        @NonNull ItemStack itemStack, @NonNull BlockState state, Level world, @NonNull BlockPos pos,
        @NonNull Player player, @NonNull InteractionHand interactionHand, @NonNull BlockHitResult blockHitResult
    ) {
        if (!world.isClientSide()) {
            MenuProvider screenHandlerFactory = state.getMenuProvider(world, pos);
            if (screenHandlerFactory != null) {
                player.openMenu(screenHandlerFactory);
            }
            tryHarvest(world, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.SHAPE;
    }

    @Override
    public boolean isCollisionShapeFullBlock(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new GreenhouseBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull Item asItem() {
        return CropariaItems.GREENHOUSE.get();
    }
}
