package cool.muyucloud.croparia.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import cool.muyucloud.croparia.api.element.block.ElementalLiquidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "renderScreenEffect", at = @At("TAIL"))
    private static void cif$renderFluidOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.player.isSpectator()) return;
        Level level = minecraft.player.getCommandSenderWorld();
        Block block = level.getBlockState(BlockPos.containing(minecraft.player.getEyePosition())).getBlock();
        if (!(block instanceof ElementalLiquidBlock eBlock)) return;
        ResourceLocation renderOverlay = SimpleArchitecturyFluidAttributesAccess.getRenderOverlayTexture(eBlock.getElement().getFluidAttr());
        if (renderOverlay == null) return;
        renderOverlay = ResourceLocation.tryBuild(renderOverlay.getNamespace(), "textures/" + renderOverlay.getPath() + ".png");
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, renderOverlay);
        BlockPos blockPos = BlockPos.containing(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ());
        float brightness = LightTexture.getBrightness(minecraft.player.level().dimensionType(), minecraft.player.level().getMaxLocalRawBrightness(blockPos));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, 0.1F);
        float yawOffset = -minecraft.player.getYRot() / 64.0F;
        float pitchOffset = minecraft.player.getXRot() / 64.0F;
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + yawOffset, 4.0F + pitchOffset);
        bufferBuilder.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(yawOffset, 4.0F + pitchOffset);
        bufferBuilder.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(yawOffset, pitchOffset);
        bufferBuilder.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + yawOffset, pitchOffset);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
