package cool.muyucloud.croparia.neoforge.mixin;

import cool.muyucloud.croparia.neoforge.CropariaFluidTypeExtension;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.fluid.forge.ArchitecturyFluidAttributesClient;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArchitecturyFluidAttributesClient.class)
public class ArchitecturyFluidAttributesClientMixin {
    /**
     * Use CropariaFluidTypeExtension for NeoForge Fluid Ex registration.
     *
     */
    @Inject(method = "initializeClient", at = @At("HEAD"), cancellable = true)
    private static void onInitClient(ArchitecturyFluidAttributes attributes, CallbackInfoReturnable<IClientFluidTypeExtensions> cir) {
        cir.setReturnValue(new CropariaFluidTypeExtension(attributes));
        cir.cancel();
    }
}
