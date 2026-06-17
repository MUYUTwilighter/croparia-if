package cool.muyucloud.croparia.api.repo.neoforge;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.neoforge.ForgeFluidSpec;
import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class FluidRepoProxy extends RepoProxy<FluidSpec> implements ResourceHandler<FluidResource> {
    public FluidRepoProxy(Repo<FluidSpec> repo) {
        super(repo);
    }

    @Override
    public @NonNull FluidResource getResource(int index) {
        Objects.checkIndex(index, this.size());
        return FluidResource.of(ForgeFluidSpec.of(this.resourceFor(index), ForgeFluidSpec.toInternalAmount(FluidType.BUCKET_VOLUME)));
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, this.size());
        return ForgeFluidSpec.toNeoAmount(this.amountFor(index, this.resourceFor(index)));
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, this.size());
        FluidSpec fluid = resource.isEmpty() ? this.resourceFor(index) : ForgeFluidSpec.from(resource.toStack(FluidType.BUCKET_VOLUME));
        return ForgeFluidSpec.toNeoAmount(this.capacityFor(index, fluid));
    }

    @Override
    public boolean isValid(int index, @NonNull FluidResource resource) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmpty(resource);
        return this.capacityFor(index, ForgeFluidSpec.from(resource.toStack(FluidType.BUCKET_VOLUME))) > 0;
    }

    @Override
    public int insert(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidSpec fluid = ForgeFluidSpec.from(resource.toStack(FluidType.BUCKET_VOLUME));
        int inserted = ForgeFluidSpec.toNeoAmount(this.simAccept(index, fluid, ForgeFluidSpec.toInternalAmount(amount)));
        if (inserted > 0) {
            this.scheduleCommit(transaction, () -> this.accept(index, fluid, ForgeFluidSpec.toInternalAmount(inserted)));
        }
        return inserted;
    }

    @Override
    public int extract(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidSpec fluid = ForgeFluidSpec.from(resource.toStack(FluidType.BUCKET_VOLUME));
        int extracted = ForgeFluidSpec.toNeoAmount(this.simConsume(index, fluid, ForgeFluidSpec.toInternalAmount(amount)));
        if (extracted > 0) {
            this.scheduleCommit(transaction, () -> this.consume(index, fluid, ForgeFluidSpec.toInternalAmount(extracted)));
        }
        return extracted;
    }

    private void scheduleCommit(TransactionContext transaction, Runnable action) {
        new RootCommitJournal(action).updateSnapshots(transaction);
    }
}
