package cool.muyucloud.croparia.api.repo.neoforge;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.platform.PlatformItemProxy;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.util.CifUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Optional;

public class PlatformItemProxyImpl implements PlatformItemProxy {
    public static PlatformItemProxyImpl of(ResourceHandler<ItemResource> handler) {
        return new PlatformItemProxyImpl(handler);
    }

    private final ResourceHandler<ItemResource> resourceHandler;

    public PlatformItemProxyImpl(ResourceHandler<ItemResource> handler) {
        this.resourceHandler = handler;
    }

    public ResourceHandler<ItemResource> getResourceHandler() {
        return this.resourceHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Repo<ItemSpec>> peel() {
        return this.getResourceHandler() instanceof Repo<?> repo ? Optional.of((Repo<ItemSpec>) repo) : Optional.empty();
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
    public long simConsume(int i, ItemSpec item, long amount) {
        return this.extractResource(i, item, amount, false);
    }

    @Override
    public long consume(int i, ItemSpec item, long amount) {
        return this.extractResource(i, item, amount, true);
    }

    @Override
    public long simAccept(int i, ItemSpec item, long amount) {
        return this.insertResource(i, item, amount, false);
    }

    @Override
    public long accept(int i, ItemSpec item, long amount) {
        return this.insertResource(i, item, amount, true);
    }

    @Override
    public long capacityFor(int i, ItemSpec item) {
        return this.getResourceHandler().getCapacityAsLong(i, ItemResource.of(item.createStack()));
    }

    @Override
    public long amountFor(int i, ItemSpec item) {
        ResourceHandler<ItemResource> handler = this.getResourceHandler();
        return ItemResource.of(item.createStack()).equals(handler.getResource(i)) ? handler.getAmountAsLong(i) : 0;
    }

    @Override
    public ItemSpec resourceFor(int i) {
        return ItemSpec.of(this.getResourceHandler().getResource(i).toStack());
    }

    private long insertResource(int i, ItemSpec item, long amount, boolean commit) {
        ResourceHandler<ItemResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int inserted = handler.insert(i, ItemResource.of(item.createStack()), CifUtil.toIntSafe(amount), transaction);
            if (commit) transaction.commit();
            return inserted;
        }
    }

    private long extractResource(int i, ItemSpec item, long amount, boolean commit) {
        ResourceHandler<ItemResource> handler = this.getResourceHandler();
        try (Transaction transaction = this.openTransaction()) {
            int extracted = handler.extract(i, ItemResource.of(item.createStack()), CifUtil.toIntSafe(amount), transaction);
            if (commit) transaction.commit();
            return extracted;
        }
    }

    @SuppressWarnings("deprecation")
    private Transaction openTransaction() {
        TransactionContext parent = Transaction.getCurrentOpenedTransaction();
        return parent == null ? Transaction.openRoot() : Transaction.open(parent);
    }
}
