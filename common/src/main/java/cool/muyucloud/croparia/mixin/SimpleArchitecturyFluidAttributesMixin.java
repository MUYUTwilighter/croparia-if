package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = SimpleArchitecturyFluidAttributes.class, remap = false)
public abstract class SimpleArchitecturyFluidAttributesMixin implements ArchitecturyFluidAttributes, SimpleArchitecturyFluidAttributesAccess {
    @Unique
    @Nullable
    private Component cif$name;
    @Unique
    @Nullable
    private ResourceLocation cif$renderOverlayTexture;

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void test(Supplier<? extends Fluid> flowingFluid, Supplier<? extends Fluid> sourceFluid, CallbackInfo ci) {
        System.out.println("SimpleArchitecturyFluidAttributes Injected");
    }

    @Override
    public Component cif$getName() {
        return this.cif$name == null ? ArchitecturyFluidAttributes.super.getName() : this.cif$name;
    }

    @Override
    public SimpleArchitecturyFluidAttributesAccess cif$setName(Component name) {
        this.cif$name = name;
        return this;
    }

    @Override
    public ResourceLocation cif$getRenderOverlayTexture() {
        return this.cif$renderOverlayTexture;
    }

    @Override
    public SimpleArchitecturyFluidAttributesAccess cif$setRenderOverlayTexture(ResourceLocation texture) {
        this.cif$renderOverlayTexture = texture;
        return this;
    }
}
