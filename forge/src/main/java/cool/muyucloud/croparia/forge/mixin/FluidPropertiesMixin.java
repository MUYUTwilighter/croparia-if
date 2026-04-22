package cool.muyucloud.croparia.forge.mixin;

import cool.muyucloud.croparia.forge.access.FluidPropertiesAccess;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FluidType.Properties.class)
public class FluidPropertiesMixin implements FluidPropertiesAccess {
    @Shadow
    private String descriptionId;

    @Unique
    @Override
    public String cif$getDescriptionId() {
        return this.descriptionId;
    }
}
