package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.crop.util.Color;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = SimpleArchitecturyFluidAttributes.class, remap = false)
public abstract class SimpleArchitecturyFluidAttributesMixin implements ArchitecturyFluidAttributes, SimpleArchitecturyFluidAttributesAccess {
    @Unique
    @Nullable
    private Component cif$name;
    @Unique
    @Nullable
    private Identifier cif$renderOverlayTexture;
    private Color cif$FogColor;

    @Override
    public Component cif$getName() {
        return this.cif$name == null ? ArchitecturyFluidAttributes.super.getName() : this.cif$name;
    }

    @Override
    public void cif$setName(Component name) {
        this.cif$name = name;
    }

    @Override
    public Identifier cif$getRenderOverlayTexture() {
        return this.cif$renderOverlayTexture;
    }

    @Override
    public void cif$setRenderOverlayTexture(Identifier texture) {
        this.cif$renderOverlayTexture = texture;
    }

    @Override
    public Color cif$getFogColor() {
        return this.cif$FogColor;
    }

    @Override
    public void cif$setFogColor(Color color) {
        this.cif$FogColor = color;
    }
}
