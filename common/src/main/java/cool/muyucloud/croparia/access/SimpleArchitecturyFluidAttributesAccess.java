package cool.muyucloud.croparia.access;

import cool.muyucloud.croparia.api.crop.util.Color;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public interface SimpleArchitecturyFluidAttributesAccess {
    Map<SimpleArchitecturyFluidAttributes, ExtraAttr> EXTRA_ATTR = new HashMap<>();

    Component cif$getName();

    void cif$setName(Component name);

    ResourceLocation cif$getRenderOverlayTexture();

    void cif$setRenderOverlayTexture(ResourceLocation texture);

    Color cif$getFogColor();

    void cif$setFogColor(Color color);

    static void setName(SimpleArchitecturyFluidAttributes instance, Component name) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            access.cif$setName(name);
        } else {
            EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).name = name;
        }
    }

    static Component getName(SimpleArchitecturyFluidAttributes instance) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            return access.cif$getName();
        } else {
            return EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).name;
        }
    }

    static void setRenderOverlayTexture(SimpleArchitecturyFluidAttributes instance, ResourceLocation texture) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            access.cif$setRenderOverlayTexture(texture);
        } else {
            EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).renderOverlayTexture = texture;
        }
    }

    static ResourceLocation getRenderOverlayTexture(SimpleArchitecturyFluidAttributes instance) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            return access.cif$getRenderOverlayTexture();
        } else {
            return EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).renderOverlayTexture;
        }
    }

    static void setFogColor(SimpleArchitecturyFluidAttributes instance, Color color) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            access.cif$setFogColor(color);
        } else {
            EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).fogColor = color;
        }
    }

    static Color getFogColor(SimpleArchitecturyFluidAttributes instance) {
        if (instance instanceof SimpleArchitecturyFluidAttributesAccess access) {
            return access.cif$getFogColor();
        } else {
            return EXTRA_ATTR.computeIfAbsent(instance, i -> new ExtraAttr()).fogColor;
        }
    }

    static SimpleArchitecturyFluidAttributesAccess cast(SimpleArchitecturyFluidAttributes instance) {
        return (SimpleArchitecturyFluidAttributesAccess) instance;
    }

    public static class ExtraAttr {
        @Nullable
        Component name;
        @Nullable
        ResourceLocation renderOverlayTexture;
        @Nullable
        Color fogColor;
    }
}
