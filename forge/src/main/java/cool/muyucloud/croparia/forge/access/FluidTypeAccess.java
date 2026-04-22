package cool.muyucloud.croparia.forge.access;

import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

public interface FluidTypeAccess {
    Map<String, ArchitecturyFluidAttributes> ATTR_HOOK = new HashMap<>();

    @Unique
    ArchitecturyFluidAttributes cif$getHookedAttr();
}
