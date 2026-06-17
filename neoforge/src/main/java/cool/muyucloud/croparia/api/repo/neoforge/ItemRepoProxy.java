package cool.muyucloud.croparia.api.repo.neoforge;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.util.CifUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class ItemRepoProxy extends RepoProxy<ItemSpec> implements ResourceHandler<ItemResource> {
    public ItemRepoProxy(Repo<ItemSpec> repo) {
        super(repo);
    }

    @Override
    public @NonNull ItemResource getResource(int index) {
        Objects.checkIndex(index, this.size());
        return ItemResource.of(this.resourceFor(index).createStack());
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, this.size());
        return this.amountFor(index, this.resourceFor(index));
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Objects.checkIndex(index, this.size());
        return resource.isEmpty() ? this.capacityFor(index) : this.capacityFor(index, ItemSpec.of(resource.toStack()));
    }

    @Override
    public boolean isValid(int index, @NonNull ItemResource resource) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmpty(resource);
        return this.capacityFor(index, ItemSpec.of(resource.toStack())) > 0;
    }

    @Override
    public int insert(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemSpec item = ItemSpec.of(resource.toStack());
        int inserted = CifUtil.toIntSafe(this.simAccept(index, item, amount));
        if (inserted > 0) {
            this.scheduleCommit(transaction, () -> this.accept(index, item, inserted));
        }
        return inserted;
    }

    @Override
    public int extract(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemSpec item = ItemSpec.of(resource.toStack());
        int extracted = CifUtil.toIntSafe(this.simConsume(index, item, amount));
        if (extracted > 0) {
            this.scheduleCommit(transaction, () -> this.consume(index, item, extracted));
        }
        return extracted;
    }

    private void scheduleCommit(TransactionContext transaction, Runnable action) {
        new RootCommitJournal(action).updateSnapshots(transaction);
    }
}
