package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.platform.PlatformFluidProxy;
import cool.muyucloud.croparia.api.resource.FabricFluidSpec;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public class PlatformFluidProxyImpl extends AbstractFabricStorageProxy<FluidSpec, FluidVariant> implements PlatformFluidProxy {
    @NotNull
    public static PlatformFluidProxyImpl of(@NotNull Storage<FluidVariant> storage) {
        return new PlatformFluidProxyImpl(storage);
    }

    public PlatformFluidProxyImpl(Storage<FluidVariant> storage) {
        super(storage);
    }

    @Override
    protected FluidSpec fromVariant(FluidVariant variant) {
        return FabricFluidSpec.from(variant);
    }

    @Override
    protected FluidVariant toVariant(FluidSpec resource) {
        return FabricFluidSpec.toVariant(resource);
    }

    @Override
    protected boolean matches(FluidVariant variant, FluidSpec resource) {
        return FabricFluidSpec.matches(variant, resource);
    }

    @Override
    public Optional<Repo<FluidSpec>> peel() {
        return super.peel();
    }
}
