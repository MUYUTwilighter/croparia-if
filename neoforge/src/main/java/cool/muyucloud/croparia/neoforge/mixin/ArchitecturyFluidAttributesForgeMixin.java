package cool.muyucloud.croparia.neoforge.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.architectury.core.fluid.ArchitecturyFluidAttributesForge", remap = false)
public class ArchitecturyFluidAttributesForgeMixin {
    @Shadow
    @Final
    private ArchitecturyFluidAttributes attributes;

    @Inject(method = "getDescription()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(CallbackInfoReturnable<Component> cir) {
        if (this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }

    @Inject(method = "getDescription(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(FluidStack stack, CallbackInfoReturnable<Component> cir) {
        if (this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }
}
