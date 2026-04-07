package cool.muyucloud.croparia.forge.mixin;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.forge.CropariaFluidTypeExtension;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
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
public class ArchitecturyFluidAttributesForgeMixin {
    @Shadow
    @Final
    private ArchitecturyFluidAttributes attributes;
    @Unique
    private CropariaFluidTypeExtension cif$extension;

    @Unique
    private CropariaFluidTypeExtension cif$getExtension() {
        if (this.cif$extension == null) {
            this.cif$extension = new CropariaFluidTypeExtension(this.attributes);
        }
        return this.cif$extension;
    }

    @Unique
    private boolean cif$shouldOverride() {
        if (!(this.attributes instanceof SimpleArchitecturyFluidAttributes)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(this.attributes.getSourceFluid());
        return id != null && CropariaIf.MOD_ID.equals(id.getNamespace());
    }

    @Inject(method = "getDescription()Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(CallbackInfoReturnable<Component> cir) {
        if (this.cif$shouldOverride() && this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }

    @Inject(method = "getDescription(Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    public void cif$getDescription(FluidStack stack, CallbackInfoReturnable<Component> cir) {
        if (this.cif$shouldOverride() && this.attributes instanceof SimpleArchitecturyFluidAttributes attr) {
            cir.setReturnValue(SimpleArchitecturyFluidAttributesAccess.getName(attr));
        }
    }

    @Inject(method = "initializeClient", at = @At("HEAD"), cancellable = true)
    public void cif$initializeClient(Consumer<IClientFluidTypeExtensions> consumer, CallbackInfo ci) {
        if (this.cif$shouldOverride()) {
            consumer.accept(this.cif$getExtension());
            ci.cancel();
        }
    }
}
