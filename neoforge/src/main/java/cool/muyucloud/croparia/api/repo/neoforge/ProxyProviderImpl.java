package cool.muyucloud.croparia.api.repo.neoforge;

import cool.muyucloud.croparia.api.repo.ProxyProvider;
import cool.muyucloud.croparia.api.repo.platform.PlatformFluidProxy;
import cool.muyucloud.croparia.api.repo.platform.PlatformItemProxy;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.neoforge.access.BlockCapabilityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Optional;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class ProxyProviderImpl {
    public static Optional<PlatformItemProxy> findItem(Level world, BlockPos pos, Direction direction) {
        Object handler = Capabilities.Item.BLOCK.getCapability(world, pos, null, null, direction);
        return handler instanceof IItemHandler itemHandler ? Optional.of(PlatformItemProxyImpl.of(itemHandler)) : Optional.empty();
    }

    public static Optional<PlatformFluidProxy> findFluid(Level world, BlockPos pos, Direction direction) {
        Object handler = Capabilities.Fluid.BLOCK.getCapability(world, pos, null, null, direction);
        return handler instanceof IFluidHandler fluidHandler ? Optional.of(PlatformFluidProxyImpl.of(fluidHandler)) : Optional.empty();
    }

    public static void registerItem(ProxyProvider<ItemSpec> provider, Block... blocks) {
        ((BlockCapabilityAccess) (Object) Capabilities.Item.BLOCK).cif$registerItem(provider, blocks);
    }

    public static void registerFluid(ProxyProvider<FluidSpec> provider, Block... blocks) {
        ((BlockCapabilityAccess) (Object) Capabilities.Fluid.BLOCK).cif$registerFluid(provider, blocks);
    }
}
