package cool.muyucloud.croparia.api.core.item.relic;

import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.PostConstants;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MidasHand extends Item {
    public MidasHand(Properties properties) {
        super(properties);
    }

    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState block = world.getBlockState(pos);
        @Nullable Player player = context.getPlayer();
        if (world.isClientSide() || player == null) return super.useOn(context);
        // Prerequisites
        if (player.totalExperience < 10) {
            Texts.overlay(player, Constants.INSUFFICIENT_XP);
            return InteractionResult.FAIL;
        }
        player.giveExperiencePoints(-10);
        // Do effect
        if (!world.getBlockState(pos).is(PostConstants.MIDAS_HAND_IMMUNE_BLOCKS)) {
            player.getCooldowns().addCooldown(context.getItemInHand(), CifUtil.toIntSafe(block.getBlock().defaultDestroyTime()));
            world.destroyBlock(pos, false);
            world.addFreshEntity(new ItemEntity(world, (double) pos.getX() + 0.5, pos.getY(), (double) pos.getZ() + 0.5, new ItemStack(Items.GOLD_INGOT)));
            return InteractionResult.SUCCESS;
        } else {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
            bolt.setPos(player.position());
            world.addFreshEntity(bolt);
            return InteractionResult.SUCCESS;
        }
    }

    public @NotNull InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity.level().isClientSide()) return super.interactLivingEntity(stack, player, entity, hand);
        if (player.getCooldowns().isOnCooldown(stack)) return super.interactLivingEntity(stack, player, entity, hand);
        int xpConsume;
        int cooldown;
        if (entity instanceof Enemy) {
            xpConsume = CifUtil.toIntSafe(entity.getHealth() * 2);
            cooldown = 400;
        } else {
            xpConsume = CifUtil.toIntSafe(entity.getHealth());
            cooldown = 200;
        }
        if (player.totalExperience < xpConsume) {
            Texts.overlay(player, Constants.INSUFFICIENT_XP);
            return InteractionResult.FAIL;
        }
        player.giveExperiencePoints(-xpConsume);
        ServerLevel world = (ServerLevel) entity.level();
        if (!entity.getType().is(PostConstants.MIDAS_HAND_IMMUNE_ENTITIES)) {
            world.destroyBlock(entity.blockPosition(), true);
            world.setBlock(entity.blockPosition(), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
            entity.remove(RemovalReason.KILLED);
            player.getCooldowns().addCooldown(stack, cooldown);
        } else {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
            bolt.setPos(entity.position());
            world.addFreshEntity(bolt);
        }
        return InteractionResult.SUCCESS;
    }
}
