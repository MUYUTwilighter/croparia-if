package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.repo.RepoProxy;
import cool.muyucloud.croparia.api.resource.TypedResource;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

abstract class AbstractFabricRepoProxy<T extends TypedResource<?>, V> extends RepoProxy<T> implements Storage<V> {
    private final ArrayList<FabricView> views = new ArrayList<>();

    protected AbstractFabricRepoProxy(Repo<T> repo) {
        super(repo);
    }

    protected abstract T fromVariant(V variant);

    protected abstract V toVariant(T resource);

    @Override
    public final long insert(V resource, long maxAmount, TransactionContext context) {
        T typedResource = this.fromVariant(resource);
        if (context == null) {
            return this.accept(typedResource, maxAmount);
        } else {
            long amount = this.simAccept(typedResource, maxAmount);
            context.addCloseCallback((ignored, result) -> {
                if (result == TransactionContext.Result.COMMITTED) {
                    this.accept(typedResource, amount);
                }
            });
            return amount;
        }
    }

    @Override
    public final long extract(V resource, long maxAmount, TransactionContext context) {
        T typedResource = this.fromVariant(resource);
        if (context == null) {
            return this.consume(typedResource, maxAmount);
        } else {
            long amount = this.simConsume(typedResource, maxAmount);
            context.addCloseCallback((ignored, result) -> {
                if (result == TransactionContext.Result.COMMITTED) {
                    this.consume(typedResource, amount);
                }
            });
            return amount;
        }
    }

    @Override
    public final @NotNull Iterator<StorageView<V>> iterator() {
        views.removeIf(view -> view.i >= this.size());
        return new FabricIterator();
    }

    private class FabricIterator implements Iterator<StorageView<V>> {
        private int i = 0;

        @Override
        public boolean hasNext() {
            return this.i < AbstractFabricRepoProxy.this.size();
        }

        @Override
        public StorageView<V> next() {
            if (!this.hasNext()) throw new IndexOutOfBoundsException("No more elements");
            if (views.size() <= this.i) {
                views.add(new FabricView(this.i));
            }
            FabricView view = views.get(this.i);
            this.i++;
            return view;
        }
    }

    private class FabricView implements StorageView<V> {
        private final int i;

        private FabricView(int i) {
            if (AbstractFabricRepoProxy.this.size() <= i) {
                throw new IllegalArgumentException("Index %s is out of bounds: %s".formatted(i, AbstractFabricRepoProxy.this.size()));
            }
            this.i = i;
        }

        @Override
        public long extract(V resource, long maxAmount, TransactionContext context) {
            T typedResource = AbstractFabricRepoProxy.this.fromVariant(resource);
            if (context == null) {
                return AbstractFabricRepoProxy.this.consume(this.i, typedResource, maxAmount);
            } else {
                long amount = AbstractFabricRepoProxy.this.simConsume(this.i, typedResource, maxAmount);
                context.addCloseCallback((ignored, result) -> {
                    if (result == TransactionContext.Result.COMMITTED) {
                        AbstractFabricRepoProxy.this.consume(this.i, typedResource, amount);
                    }
                });
                return amount;
            }
        }

        @Override
        public boolean isResourceBlank() {
            return AbstractFabricRepoProxy.this.isEmpty(this.i);
        }

        @Override
        public V getResource() {
            return AbstractFabricRepoProxy.this.toVariant(AbstractFabricRepoProxy.this.resourceFor(this.i));
        }

        @Override
        public long getAmount() {
            return AbstractFabricRepoProxy.this.amountFor(this.i, AbstractFabricRepoProxy.this.resourceFor(this.i));
        }

        @Override
        public long getCapacity() {
            return AbstractFabricRepoProxy.this.capacityFor(this.i, AbstractFabricRepoProxy.this.resourceFor(this.i));
        }
    }
}
