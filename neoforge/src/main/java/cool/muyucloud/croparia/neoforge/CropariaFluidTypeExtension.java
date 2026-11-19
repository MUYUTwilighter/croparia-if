package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.crop.asset.Color;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

public class CropariaFluidTypeExtension implements IClientFluidTypeExtensions {
    private final ArchitecturyFluidAttributes attr;

    public CropariaFluidTypeExtension(ArchitecturyFluidAttributes attr) {
        this.attr = attr;
    }

    @Override
    public @Nullable Identifier getRenderOverlayTexture(@NotNull Minecraft mc) {
        if (this.attr instanceof SimpleArchitecturyFluidAttributes simpleAttr) {
            Identifier renderOverlay = SimpleArchitecturyFluidAttributesAccess.getRenderOverlayTexture(simpleAttr);
            if (renderOverlay == null) return null;
            return Identifier.tryBuild(renderOverlay.getNamespace(), "textures/" + renderOverlay.getPath() + ".png");
        }
        return null;
    }

    @Override
    public void modifyFogColor(@NotNull Camera camera, float partialTick, @NotNull ClientLevel level, int renderDistance, float darkenWorldAmount, @NotNull Vector4f fluidFogColor) {
        if (this.attr instanceof SimpleArchitecturyFluidAttributes simpleAttr) {
            Color color = SimpleArchitecturyFluidAttributesAccess.getFogColor(simpleAttr);
            if (color != null) fluidFogColor.set(color.toVector3f(), fluidFogColor.w);
        }
    }
}
