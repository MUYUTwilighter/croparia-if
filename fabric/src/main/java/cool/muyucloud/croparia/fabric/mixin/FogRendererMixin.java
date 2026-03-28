package cool.muyucloud.croparia.fabric.mixin;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.crop.util.Color;
import cool.muyucloud.croparia.api.element.block.ElementalLiquidBlock;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Unique
    private static final float CIF_FOG_RED_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_GREEN_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_BLUE_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_START = -8.0F;
    @Unique
    private static final float CIF_FOG_END = 12.0F;

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void cif$setupColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, CallbackInfo ci) {
        ElementalLiquidBlock block = cif$getElementalLiquidBlock(camera, level);
        if (block == null) return;

        Color color = SimpleArchitecturyFluidAttributesAccess.getFogColor(block.getElement().getFluidAttr());
        if (color == null) return;

        fogRed = ((color.getValue() >> 16) & 0xFF) / 255.0F * CIF_FOG_RED_MULTIPLIER;
        fogGreen = ((color.getValue() >> 8) & 0xFF) / 255.0F * CIF_FOG_GREEN_MULTIPLIER;
        fogBlue = (color.getValue() & 0xFF) / 255.0F * CIF_FOG_BLUE_MULTIPLIER;
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void cif$setupFog(Camera camera, FogRenderer.FogMode fogMode, float renderDistance, boolean isFoggy, float partialTick, CallbackInfo ci) {
        ElementalLiquidBlock block = cif$getElementalLiquidBlock(camera, null);
        if (block == null) return;

        RenderSystem.setShaderFogStart(CIF_FOG_START);
        RenderSystem.setShaderFogEnd(CIF_FOG_END);
        RenderSystem.setShaderFogShape(FogShape.CYLINDER);
    }

    @Unique
    @Nullable
    private static ElementalLiquidBlock cif$getElementalLiquidBlock(Camera camera, @Nullable ClientLevel level) {
        ClientLevel actualLevel = level;
        if (actualLevel == null) {
            if (!(camera.getEntity().level() instanceof ClientLevel clientLevel)) return null;
            actualLevel = clientLevel;
        }
        BlockPos blockPos = BlockPos.containing(camera.getPosition());
        Block block = actualLevel.getBlockState(blockPos).getBlock();
        return block instanceof ElementalLiquidBlock elementalLiquidBlock ? elementalLiquidBlock : null;
    }
}
