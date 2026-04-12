package cool.muyucloud.croparia.fabric.mixin;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.crop.util.Color;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static final float CIF_FOG_RED_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_GREEN_MULTIPLIER = 0.9F;
    @Unique
    private static final float CIF_FOG_BLUE_MULTIPLIER = 0.9F;

    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void cif$computeFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            CallbackInfoReturnable<Vector4f> cir
    ) {
        ElementalLiquidBlock block = cif$getElementalLiquidBlock(camera, level);
        if (block == null) return;

        Color color = SimpleArchitecturyFluidAttributesAccess.getFogColor(block.getElement().getFluidAttr());
        if (color == null) return;

        cir.setReturnValue(new Vector4f(
                ((color.getValue() >> 16) & 0xFF) / 255.0F * CIF_FOG_RED_MULTIPLIER,
                ((color.getValue() >> 8) & 0xFF) / 255.0F * CIF_FOG_GREEN_MULTIPLIER,
                (color.getValue() & 0xFF) / 255.0F * CIF_FOG_BLUE_MULTIPLIER,
                1.0F
        ));
    }

    @Unique
    @Nullable
    private static ElementalLiquidBlock cif$getElementalLiquidBlock(Camera camera, @Nullable ClientLevel level) {
        ClientLevel actualLevel = level;
        if (actualLevel == null) {
            if (!(camera.entity().level() instanceof ClientLevel clientLevel)) return null;
            actualLevel = clientLevel;
        }

        BlockPos blockPos = BlockPos.containing(camera.position());
        Block block = actualLevel.getBlockState(blockPos).getBlock();
        return block instanceof ElementalLiquidBlock elementalLiquidBlock ? elementalLiquidBlock : null;
    }
}
