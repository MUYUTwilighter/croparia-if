package cool.muyucloud.croparia.access;

import cool.muyucloud.croparia.api.crop.util.Color;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public interface SimpleArchitecturyFluidAttributesAccess {
    Component cif$getName();

    void cif$setName(Component name);

    ResourceLocation cif$getRenderOverlayTexture();

    void cif$setRenderOverlayTexture(ResourceLocation texture);

    Color cif$getFogColor();

    void cif$setFogColor(Color color);

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

    static Color getFogColor(SimpleArchitecturyFluidAttributes instance) {
        return cast(instance).cif$getFogColor();
    }

    static void setFogColor(SimpleArchitecturyFluidAttributes instance, Color color) {
        cast(instance).cif$setFogColor(color);
    }
}
