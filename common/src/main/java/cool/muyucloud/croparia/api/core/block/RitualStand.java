package cool.muyucloud.croparia.api.core.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.container.RitualContainer;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.ItemPlaceable;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class RitualStand extends Block implements ItemPlaceable {
    protected final VoxelShape SHAPE = Block.box(0.0, 0.3, 0.0, 16.0, 6.0, 16.0);
    private final int tier;
    private ItemEntity lastCalled = null;
    private long last = 0;

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
            CifUtil.placeItem(world, pos, itemStack);
            return InteractionResult.CONSUME;
        }
        return super.useItemOn(itemStack, blockState, world, pos, player, hand, blockHitResult);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        long now = (long) (System.currentTimeMillis() / world.tickRateManager().millisecondsPerTick());
        if (entity instanceof ItemEntity itemEntity &&
            (lastCalled == null || lastCalled.getOnPos().equals(pos) || now != last)
            && world instanceof ServerLevel level && CropariaIf.CONFIG.getRitual()) {
            int hashed = (int) (Math.abs(pos.hashCode()) % level.tickRateManager().tickrate());
            int offset = (int) (now % level.tickRateManager().tickrate());
            if (hashed != offset) {
                return;
            }
            lastCalled = itemEntity;
            last = now;
            RecipeManager recipeManager = level.getServer().getRecipeManager();
            recipeManager.getRecipeFor(
                Recipes.RITUAL_STRUCTURE, new RitualStructureContainer(level.getBlockState(pos)), level
            ).map(RecipeHolder::value).map(structure -> structure.validate(pos, level)).ifPresentOrElse(
                r -> r.ifSuccessOrElse(matched -> {
                    RitualContainer matcher = RitualContainer.of(level, pos, matched);
                    recipeManager.getRecipeFor(Recipes.RITUAL, matcher, level).map(RecipeHolder::value).ifPresentOrElse(ritual -> {
                        ItemStack result = ritual.assemble(matcher);
                        CifUtil.exportItem(level, pos, result, itemEntity.getOwner() instanceof Player player ? player : null);
                        this.playSound(level, pos);
                    }, () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.rejected")));
                }, () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.bad"))),
                () -> this.tryTell(itemEntity, Texts.translatable("overlay.croparia.ritual.404"))
            );
        }
    }

    protected void tryTell(ItemEntity item, Component msg) {
        if (item.getOwner() instanceof Player player) Texts.overlay(player, msg);
    }

    protected void playSound(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        level.playSound(null, pos, SoundEvent.createVariableRangeEvent(CropariaIf.of("")), SoundSource.BLOCKS);
    }

    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.SHAPE;
    }

    public int getTier() {
        return this.tier;
    }

    @Override
    public void placeItem(Level world, BlockPos pos, ItemStack stack) {
        CifUtil.placeItem(world, pos, stack);
    }
}
