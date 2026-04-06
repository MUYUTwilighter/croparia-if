package cool.muyucloud.croparia.api.resource.neoforge;

import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import cool.muyucloud.croparia.util.CifUtil;
import net.minecraftforge.fluids.FluidStack;

@SuppressWarnings("unused")
public class ForgeFluidSpec {
    public static final long FORGE_TO_INTERNAL_RATIO = 81L;

    public static int toForgeAmount(long internalAmount) {
        return CifUtil.toIntSafe(internalAmount / FORGE_TO_INTERNAL_RATIO);
    }

    public static long toInternalAmount(long forgeAmount) {
        return forgeAmount * FORGE_TO_INTERNAL_RATIO;
    }

    public static FluidStack of(FluidSpec fluidSpec, long amount) {
        return new FluidStack(fluidSpec.getResource(), toForgeAmount(amount), fluidSpec.getNbt().orElse(null));
    }

    public static FluidStack of(FluidSpec fluidSpec, int amount) {
        return new FluidStack(fluidSpec.getResource(), amount, fluidSpec.getNbt().orElse(null));
    }

    public static FluidSpec from(FluidStack stack) {
        return new FluidSpec(stack.getFluid(), stack.getTag());
    }

    public static boolean matches(FluidSpec a, FluidStack b) {
        return a.getResource() == b.getFluid() && a.getTagOrEmpty().equals(b.getTag() == null ? new net.minecraft.nbt.CompoundTag() : b.getTag());
    }

    public static boolean matches(FluidStack a, FluidSpec b) {
        return b.getResource() == a.getFluid() && b.getTagOrEmpty().equals(a.getTag() == null ? new net.minecraft.nbt.CompoundTag() : a.getTag());
    }
}
