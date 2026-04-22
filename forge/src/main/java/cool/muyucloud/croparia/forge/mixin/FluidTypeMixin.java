package cool.muyucloud.croparia.forge.mixin;

import cool.muyucloud.croparia.forge.access.FluidTypeAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FluidType.class)
public class FluidTypeMixin implements FluidTypeAccess {
    @Shadow
    private String descriptionId;

    @Override
    public ArchitecturyFluidAttributes cif$getHookedAttr() {
        return ATTR_HOOK.get(this.descriptionId);
    }
}
