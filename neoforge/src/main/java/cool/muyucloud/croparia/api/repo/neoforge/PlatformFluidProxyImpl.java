package cool.muyucloud.croparia.api.repo.neoforge;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.platform.PlatformFluidProxy;
import cool.muyucloud.croparia.api.resource.neoforge.ForgeFluidSpec;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Optional;

public class PlatformFluidProxyImpl implements PlatformFluidProxy {
    public static PlatformFluidProxyImpl of(ResourceHandler<FluidResource> handler) {
        return new PlatformFluidProxyImpl(handler);
    }

    private final ResourceHandler<FluidResource> resourceHandler;

    public PlatformFluidProxyImpl(ResourceHandler<FluidResource> handler) {
        this.resourceHandler = handler;
    }

    public ResourceHandler<FluidResource> getResourceHandler() {
        return this.resourceHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Repo<FluidSpec>> peel() {
        return this.getResourceHandler() instanceof Repo<?> repo ? Optional.of((Repo<FluidSpec>) repo) : Optional.empty();
    }

    @Override
    public int size() {
        return this.getResourceHandler().size();
    }

    @Override
    public boolean isEmpty(int i) {
        return this.getResourceHandler().getResource(i).isEmpty() || this.getResourceHandler().getAmountAsLong(i) <= 0;
    }

    @Override
    public long simConsume(FluidSpec fluid, long amount) {
        return this.extractResource(fluid, amount, false);
    }

    @Override
    public long simConsume(int i, FluidSpec resource, long amount) {
        return this.extractResource(i, resource, amount, false);
    }

    @Override
    public long consume(FluidSpec resource, long amount) {
        return this.extractResource(resource, amount, true);
    }

    @Override
    public long consume(int i, FluidSpec resource, long amount) {
        return this.extractResource(i, resource, amount, true);
    }

    @Override
    public long simAccept(FluidSpec resource, long amount) {
        return this.insertResource(resource, amount, false);
    }

    @Override
    public long simAccept(int i, FluidSpec resource, long amount) {
        return this.insertResource(i, resource, amount, false);
    }

    @Override
    public long accept(FluidSpec fluid, long amount) {
        return this.insertResource(fluid, amount, true);
    }

    @Override
    public long accept(int i, FluidSpec fluid, long amount) {
        return this.insertResource(i, fluid, amount, true);
    }

    @Override
    public long capacityFor(int i, FluidSpec fluid) {
        return ForgeFluidSpec.toInternalAmount(this.getResourceHandler().getCapacityAsLong(i, FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME)))));
    }

    @Override
    public long amountFor(int i, FluidSpec fluid) {
        ResourceHandler<FluidResource> handler = this.getResourceHandler();
        FluidResource resource = handler.getResource(i);
        return resource.equals(FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME))))
            ? ForgeFluidSpec.toInternalAmount(handler.getAmountAsLong(i)) : 0;
    }

    @Override
    public FluidSpec resourceFor(int i) {
        return ForgeFluidSpec.from(this.getResourceHandler().getResource(i).toStack(FluidType.BUCKET_VOLUME));
    }

    private long insertResource(FluidSpec fluid, long amount, boolean commit) {
        ResourceHandler<FluidResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int inserted = handler.insert(FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME))), ForgeFluidSpec.toNeoAmount(amount), transaction);
            if (commit) transaction.commit();
            return ForgeFluidSpec.toInternalAmount(inserted);
        }
    }

    private long insertResource(int i, FluidSpec fluid, long amount, boolean commit) {
        ResourceHandler<FluidResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int inserted = handler.insert(i, FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME))), ForgeFluidSpec.toNeoAmount(amount), transaction);
            if (commit) transaction.commit();
            return ForgeFluidSpec.toInternalAmount(inserted);
        }
    }

    private long extractResource(FluidSpec fluid, long amount, boolean commit) {
        ResourceHandler<FluidResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int extracted = handler.extract(FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME))), ForgeFluidSpec.toNeoAmount(amount), transaction);
            if (commit) transaction.commit();
            return ForgeFluidSpec.toInternalAmount(extracted);
        }
    }

    private long extractResource(int i, FluidSpec fluid, long amount, boolean commit) {
        ResourceHandler<FluidResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int extracted = handler.extract(i, FluidResource.of(ForgeFluidSpec.of(fluid, ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME))), ForgeFluidSpec.toNeoAmount(amount), transaction);
            if (commit) transaction.commit();
            return ForgeFluidSpec.toInternalAmount(extracted);
        }
    }

    @SuppressWarnings("deprecation")
    private Transaction openTransaction() {
        TransactionContext parent = Transaction.getCurrentOpenedTransaction();
        return parent == null ? Transaction.openRoot() : Transaction.open(parent);
    }
}
