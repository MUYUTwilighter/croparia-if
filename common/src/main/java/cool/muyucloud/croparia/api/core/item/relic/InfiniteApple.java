package cool.muyucloud.croparia.api.core.item.relic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class InfiniteApple extends Item {
    public InfiniteApple(Properties properties) {
        super(properties);
    }

    public @NonNull ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (user instanceof Player player) {
            boolean old = player.getAbilities().instabuild;
            player.getAbilities().instabuild = true;
            super.finishUsingItem(stack, world, user);
            player.getAbilities().instabuild = old;
            if (!world.isClientSide()) {
                player.getCooldowns().addCooldown(stack, 200);
            }
        }
        return stack;
    }
}
