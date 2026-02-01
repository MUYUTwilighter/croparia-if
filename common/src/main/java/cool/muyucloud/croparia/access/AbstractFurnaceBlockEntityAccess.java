package cool.muyucloud.croparia.access;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Unique;

public interface AbstractFurnaceBlockEntityAccess {
    @Unique
    int cif$getBurnDuration(ItemStack stack);
}
