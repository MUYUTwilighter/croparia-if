package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.access.SimpleArchitecturyFluidAttributesAccess;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CropariaFluidTypeExtension implements IClientFluidTypeExtensions {
    private ArchitecturyFluidAttributes attr;

    public CropariaFluidTypeExtension(ArchitecturyFluidAttributes attr) {
        this.attr = attr;
    }
    
    public int getTintColor() {
        return this.attr.getColor();
    }

    public @NotNull ResourceLocation getStillTexture() {
        return this.attr.getSourceTexture();
    }

    public @NotNull ResourceLocation getFlowingTexture() {
        return this.attr.getFlowingTexture();
    }

    public @Nullable ResourceLocation getOverlayTexture() {
        return this.attr.getOverlayTexture();
    }

    public @NotNull ResourceLocation getStillTexture(@NotNull FluidState state, @NotNull BlockAndTintGetter getter, @NotNull BlockPos pos) {
        return this.attr.getSourceTexture(state, getter, pos);
    }

    public @NotNull ResourceLocation getFlowingTexture(@NotNull FluidState state, @NotNull BlockAndTintGetter getter, @NotNull BlockPos pos) {
        return this.attr.getFlowingTexture(state, getter, pos);
    }

    public @NotNull ResourceLocation getOverlayTexture(@NotNull FluidState state, @NotNull BlockAndTintGetter getter, @NotNull BlockPos pos) {
        return this.attr.getOverlayTexture(state, getter, pos);
    }

    public int getTintColor(@NotNull FluidState state, @NotNull BlockAndTintGetter getter, @NotNull BlockPos pos) {
        return this.attr.getColor(state, getter, pos);
    }

    public int getTintColor(@NotNull FluidStack stack) {
        return this.attr.getColor(this.convertSafe(stack));
    }

    public @NotNull ResourceLocation getStillTexture(@NotNull FluidStack stack) {
        return this.attr.getSourceTexture(this.convertSafe(stack));
    }

    public @NotNull ResourceLocation getFlowingTexture(@NotNull FluidStack stack) {
        return this.attr.getFlowingTexture(this.convertSafe(stack));
    }

    public @NotNull ResourceLocation getOverlayTexture(@NotNull FluidStack stack) {
        return this.attr.getOverlayTexture(this.convertSafe(stack)) ;
    }

    @Override
    public @Nullable ResourceLocation getRenderOverlayTexture(@NotNull Minecraft mc) {
        if (this.attr instanceof SimpleArchitecturyFluidAttributes simpleAttr) {
            ResourceLocation renderOverlay = SimpleArchitecturyFluidAttributesAccess.getRenderOverlayTexture(simpleAttr);
            return ResourceLocation.tryBuild(renderOverlay.getNamespace(), "textures/" + renderOverlay.getPath() + ".png");
        }
        return null;
    }

    public dev.architectury.fluid.@Nullable FluidStack convertSafe(@Nullable FluidStack stack) {
        return stack == null ? null : FluidStackHooksForge.fromForge(stack);
    }

    public dev.architectury.fluid.@Nullable FluidStack convertSafe(@Nullable FluidState state) {
        return state == null ? null : dev.architectury.fluid.FluidStack.create(state.getType(), dev.architectury.fluid.FluidStack.bucketAmount());
    }
}
