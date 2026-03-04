package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.resource.FabricFluidSpec;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

public class FluidRepoProxy extends AbstractFabricRepoProxy<FluidSpec, FluidVariant> {
    public FluidRepoProxy(Repo<FluidSpec> repo) {
        super(repo);
    }

    @Override
    protected FluidSpec fromVariant(FluidVariant variant) {
        return FabricFluidSpec.from(variant);
    }

    @Override
    protected FluidVariant toVariant(FluidSpec resource) {
        return FabricFluidSpec.toVariant(resource);
    }
}
