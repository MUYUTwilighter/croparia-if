package cool.muyucloud.croparia.fabric.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.crop.asset.Color;
import cool.muyucloud.croparia.api.element.block.ElementalLiquidBlock;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static final float CIF_FOG_RED_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_GREEN_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_BLUE_MULTIPLIER = 0.9F;

    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void cif$computeFogColor(
            Camera camera,
            float partialTicks,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector4f dest,
            CallbackInfo ci
    ) {
        ElementalLiquidBlock block = cif$getElementalLiquidBlock(camera, level);
        if (block == null) return;

        Color color = SimpleArchitecturyFluidAttributesAccess.getFogColor(block.getElement().getFluidAttr());
        if (color == null) return;

        dest.set(
                ((color.getValue() >> 16) & 0xFF) / 255.0F * CIF_FOG_RED_MULTIPLIER,
                ((color.getValue() >> 8) & 0xFF) / 255.0F * CIF_FOG_GREEN_MULTIPLIER,
                (color.getValue() & 0xFF) / 255.0F * CIF_FOG_BLUE_MULTIPLIER,
                1.0F
        );
    }

    @Unique
    @Nullable
    private static ElementalLiquidBlock cif$getElementalLiquidBlock(Camera camera, @Nullable ClientLevel level) {
        ClientLevel actualLevel = level;
        if (actualLevel == null) {
            var entity = camera.entity();
            if (entity == null || !(entity.level() instanceof ClientLevel clientLevel)) return null;
            actualLevel = clientLevel;
        }

        BlockPos blockPos = BlockPos.containing(camera.position());
        Block block = actualLevel.getBlockState(blockPos).getBlock();
        return block instanceof ElementalLiquidBlock elementalLiquidBlock ? elementalLiquidBlock : null;
    }
}
