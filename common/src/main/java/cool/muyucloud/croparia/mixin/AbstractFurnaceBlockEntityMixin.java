package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.AbstractFurnaceBlockEntityAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements AbstractFurnaceBlockEntityAccess {
    @Shadow
    protected abstract int getBurnDuration(ItemStack fuel);

    @Override
    @Unique
    public int cif$getBurnDuration(ItemStack stack) {
        return getBurnDuration(stack);
    }
}
