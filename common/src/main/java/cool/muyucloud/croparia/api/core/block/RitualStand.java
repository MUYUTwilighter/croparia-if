package cool.muyucloud.croparia.api.core.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.component.TargetPos;
import cool.muyucloud.croparia.api.core.entity.FakePlayer;
import cool.muyucloud.croparia.api.core.recipe.container.RitualContainer;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.ItemPlaceable;
import cool.muyucloud.croparia.util.supplier.SemiSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RitualStand extends Block implements ItemPlaceable {
    public static final SemiSupplier<Map<TargetPos, Set<ItemEntity>>> CACHED_ITEMS = SemiSupplier.of(HashMap::new);
    private static long LAST_CLEAR = 0L;

    protected static final VoxelShape SHAPE = Block.box(0.0, 0.3, 0.0, 16.0, 6.0, 16.0);
    private final int tier;

    public RitualStand(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected @NotNull InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (itemStack.getItem() == CropariaItems.RECIPE_WIZARD.get()) {
                return InteractionResult.PASS;
            }
            this.placeItem(world, pos, itemStack, player);
            return InteractionResult.CONSUME;
        }
        return super.useItemOn(itemStack, blockState, world, pos, player, hand, blockHitResult);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - LAST_CLEAR > 1000L) {
            LAST_CLEAR = currentTime;
            CACHED_ITEMS.refresh();
        }
        if (entity instanceof ItemEntity itemEntity && CropariaIf.CONFIG.getRitual() && world instanceof ServerLevel level) {
            Map<TargetPos, Set<ItemEntity>> cached = CACHED_ITEMS.get();
            TargetPos targetPos = new TargetPos(level, pos);
            Set<ItemEntity> cachedItems = cached.computeIfAbsent(targetPos, k -> Set.of())
                .stream().filter(item -> item.isAlive() && !item.getItem().isEmpty())
                .collect(Collectors.toSet());
            cached.put(targetPos, cachedItems);
            if (cachedItems.contains(itemEntity)) return;
            cachedItems.add(itemEntity);
            Recipes.RITUAL_STRUCTURE.find(new RitualStructureContainer(level.getBlockState(pos)), level).map(
                structure -> structure.validate(pos, level)
            ).ifPresentOrElse(
                r -> r.ifSuccessOrElse(matched -> {
                    RitualContainer matcher = RitualContainer.of(level.getBlockState(pos), cachedItems, matched);
                    Recipes.RITUAL.find(matcher, level).ifPresentOrElse(ritual -> {
                        ItemStack result = ritual.assemble(matcher);
                        if (result.getItem() instanceof SpawnEggItem) {
                            FakePlayer.useAllItemsOn(level, pos.above(), result);
                        }
                        cachedItems.remove(itemEntity);
                        CifUtil.exportItem(level, pos, result, itemEntity.getOwner() instanceof Player player ? player : null);
                        this.playSound(level, pos);
                    }, () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.rejected")));
                }, () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.bad"))),
                () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.404"))
            );
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        TargetPos targetPos = new TargetPos(level, pos);
        CACHED_ITEMS.get().remove(targetPos);
    }

    protected void tryTell(ItemEntity item, Component msg) {
        if (item.getOwner() instanceof Player player) Texts.overlay(player, msg);
    }

    protected void playSound(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        level.playSound(null, pos, SoundEvent.createVariableRangeEvent(CropariaIf.of("block.ritual.craft")), SoundSource.BLOCKS);
    }

    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public int getTier() {
        return this.tier;
    }
}
