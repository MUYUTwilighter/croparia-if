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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Optional;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class ProxyProviderImpl {
    public static Optional<PlatformItemProxy> findItem(Level world, BlockPos pos, Direction direction) {
        ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(world, pos, null, null, direction);
        return handler == null ? Optional.empty() : Optional.of(PlatformItemProxyImpl.of(handler));
    }

    public static Optional<PlatformFluidProxy> findFluid(Level world, BlockPos pos, Direction direction) {
        ResourceHandler<FluidResource> handler = Capabilities.Fluid.BLOCK.getCapability(world, pos, null, null, direction);
        return handler == null ? Optional.empty() : Optional.of(PlatformFluidProxyImpl.of(handler));
    }

    public static void registerItem(ProxyProvider<ItemSpec> provider, Block... blocks) {
        ((BlockCapabilityAccess) (Object) Capabilities.Item.BLOCK).cif$registerItem(provider, blocks);
    }

    public static void registerFluid(ProxyProvider<FluidSpec> provider, Block... blocks) {
        ((BlockCapabilityAccess) (Object) Capabilities.Fluid.BLOCK).cif$registerFluid(provider, blocks);
    }
}
