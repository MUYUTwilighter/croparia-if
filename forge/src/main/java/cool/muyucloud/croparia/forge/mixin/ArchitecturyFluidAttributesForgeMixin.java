package cool.muyucloud.croparia.forge.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.forge.CropariaFluidTypeExtension;
import cool.muyucloud.croparia.forge.access.ArchitecturyFluidAttributesForgeAccess;
import cool.muyucloud.croparia.util.supplier.Mappable;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(targets = "dev.architectury.core.fluid.ArchitecturyFluidAttributesForge", remap = false)
public class ArchitecturyFluidAttributesForgeMixin implements ArchitecturyFluidAttributesForgeAccess {
    @Shadow
    @Final
    private ArchitecturyFluidAttributes attributes;
    @Unique
    private Mappable<CropariaFluidTypeExtension> cif$extension = () -> new CropariaFluidTypeExtension(this.cif$GetAttributes());

    @Unique
    public ArchitecturyFluidAttributes cif$GetAttributes() {
        return this.attributes;
    }

    @Override
    public CropariaFluidTypeExtension cif$getExtension() {
        return this.cif$extension.get();
    }

    @Inject(method = "getDescription()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(CallbackInfoReturnable<Component> cir) {
        if (this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }

    @Inject(method = "getDescription(Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(FluidStack stack, CallbackInfoReturnable<Component> cir) {
        if (this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }

    @Inject(method = "initializeClient", at = @At("HEAD"), cancellable = true)
    public void cif$initializeClient(Consumer<IClientFluidTypeExtensions> consumer, CallbackInfo ci) {
        consumer.accept(cif$extension.get());
        ci.cancel();
    }
}
