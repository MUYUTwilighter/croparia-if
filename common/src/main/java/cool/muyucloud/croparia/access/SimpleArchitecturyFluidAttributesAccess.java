package cool.muyucloud.croparia.access;

import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface SimpleArchitecturyFluidAttributesAccess {
    Component cif$getName();

    SimpleArchitecturyFluidAttributesAccess cif$setName(Component name);

    ResourceLocation cif$getRenderOverlayTexture();

    SimpleArchitecturyFluidAttributesAccess cif$setRenderOverlayTexture(ResourceLocation texture);

    static void setName(SimpleArchitecturyFluidAttributes instance, Component name) {
        cast(instance).cif$setName(name);
    }

    static Component getName(SimpleArchitecturyFluidAttributes instance) {
        return cast(instance).cif$getName();
    }

    static void setRenderOverlayTexture(SimpleArchitecturyFluidAttributes instance, ResourceLocation texture) {
        cast(instance).cif$setRenderOverlayTexture(texture);
    }

    static ResourceLocation getRenderOverlayTexture(SimpleArchitecturyFluidAttributes instance) {
        return cast(instance).cif$getRenderOverlayTexture();
    }

    static SimpleArchitecturyFluidAttributesAccess cast(SimpleArchitecturyFluidAttributes instance) {
        return (SimpleArchitecturyFluidAttributesAccess) instance;
    }
}
