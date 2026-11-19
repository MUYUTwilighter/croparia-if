package cool.muyucloud.croparia.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.element.block.ElementalLiquidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private MultiBufferSource bufferSource;

    @Inject(method = "renderScreenEffect", at = @At("TAIL"))
    private void cif$renderFluidOverlay(boolean isFirstPerson, boolean isSleeping, float partialTicks, SubmitNodeCollector submitNodeCollector, boolean hideGui, CallbackInfo ci) {
        if (!isFirstPerson || isSleeping || this.minecraft.player == null || this.minecraft.player.isSpectator()) return;

        Block block = this.minecraft.player.level().getBlockState(BlockPos.containing(this.minecraft.player.getEyePosition())).getBlock();
        if (!(block instanceof ElementalLiquidBlock eBlock)) return;

        Identifier renderOverlay = SimpleArchitecturyFluidAttributesAccess.getRenderOverlayTexture(eBlock.getElement().getFluidAttr());
        if (renderOverlay == null) return;

        renderOverlay = Identifier.fromNamespaceAndPath(renderOverlay.getNamespace(), "textures/" + renderOverlay.getPath() + ".png");
        cif$renderOverlay(this.minecraft, this.bufferSource, renderOverlay);
    }

    @Unique
    private static void cif$renderOverlay(Minecraft minecraft, MultiBufferSource bufferSource, Identifier texture) {
        var player = minecraft.player;
        if (player == null) return;
        BlockPos blockPos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = Lightmap.getBrightness(
                player.level().dimensionType(),
                player.level().getMaxLocalRawBrightness(blockPos)
        );
        int color = ARGB.colorFromFloat(0.1F, brightness, brightness, brightness);
        float yawOffset = -player.getYRot() / 64.0F;
        float pitchOffset = player.getXRot() / 64.0F;
        PoseStack poseStack = new PoseStack();
        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.blockScreenEffect(texture));
        vertexConsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + yawOffset, 4.0F + pitchOffset).setColor(color);
        vertexConsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(yawOffset, 4.0F + pitchOffset).setColor(color);
        vertexConsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(yawOffset, pitchOffset).setColor(color);
        vertexConsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + yawOffset, pitchOffset).setColor(color);
    }
}
