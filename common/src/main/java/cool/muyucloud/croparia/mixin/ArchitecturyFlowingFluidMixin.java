package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.ArchitecturyFlowingFluidAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ArchitecturyFlowingFluid.class, remap = false)
public interface ArchitecturyFlowingFluidMixin extends ArchitecturyFlowingFluidAccess {
    @Accessor("attributes")
    ArchitecturyFluidAttributes cif$getAttributes();
}
