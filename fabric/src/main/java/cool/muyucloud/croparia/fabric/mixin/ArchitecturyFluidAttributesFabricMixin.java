package cool.muyucloud.croparia.fabric.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.architectury.core.fluid.fabric.ArchitecturyFluidAttributesFabric", remap = false)
public class ArchitecturyFluidAttributesFabricMixin implements FluidVariantAttributeHandler {
    @Shadow
    @Final
    private ArchitecturyFluidAttributes attributes;

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    public void cif$getName(FluidVariant variant, CallbackInfoReturnable<Component> cir) {
        if (this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }
}
