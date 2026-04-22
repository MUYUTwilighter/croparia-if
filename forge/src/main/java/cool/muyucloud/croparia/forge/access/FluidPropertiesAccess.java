package cool.muyucloud.croparia.forge.access;

import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Unique;

public interface FluidPropertiesAccess {
    @Unique
    String cif$getDescriptionId();

    static String getDescriptionId(FluidType.Properties properties) {
        return ((FluidPropertiesAccess) (Object) properties).cif$getDescriptionId();
    }
}
