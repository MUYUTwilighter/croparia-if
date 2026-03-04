package cool.muyucloud.croparia.api.repo.fabric;

import cool.muyucloud.croparia.api.repo.Repo;
import cool.muyucloud.croparia.api.resource.TypedResource;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;

abstract class AbstractFabricStorageProxy<T extends TypedResource<?>, V> implements Repo<T> {
    private final Storage<V> storage;

    protected AbstractFabricStorageProxy(@NotNull Storage<V> storage) {
        this.storage = storage;
    }

    protected final Storage<V> get() {
        return this.storage;
    }

    @Nullable
    protected final StorageView<V> get(int i) {
        int v = -1;
        Iterator<StorageView<V>> iterator = this.storage.iterator();
        StorageView<V> view = null;
        while (iterator.hasNext() && i > v) {
            v++;
            view = iterator.next();
        }
        if (i != v || view == null) {
            return null;
        } else {
            return view;
        }
    }

    protected abstract T fromVariant(V variant);

    protected abstract V toVariant(T resource);

    protected abstract boolean matches(V variant, T resource);

    @SuppressWarnings("unchecked")
    public Optional<Repo<T>> peel() {
        return this.get() instanceof Repo<?> repo ? Optional.of((Repo<T>) repo) : Optional.empty();
    }

    @Override
    public final int size() {
        int i = 0;
        for (StorageView<V> ignored : this.get()) {
            i++;
        }
        return i;
    }

    @Override
    public final boolean isEmpty(int i) {
        StorageView<V> view = this.get(i);
        return view == null || view.isResourceBlank();
    }

    @Nullable
    @Override
    public final T resourceFor(int i) {
        StorageView<V> view = this.get(i);
        if (view == null) {
            return null;
        } else {
            return this.fromVariant(view.getResource());
        }
    }

    @Override
    public final long simConsume(int i, T resource, long amount) {
        if (!this.get().supportsExtraction()) {
            return 0L;
        }
        StorageView<V> view = this.get(i);
        if (view == null) {
            return 0L;
        } else {
            return StorageUtil.simulateExtract(view, this.toVariant(resource), amount, null);
        }
    }

    @Override
    public final long consume(T resource, long amount) {
        if (!this.get().supportsExtraction()) {
            return 0L;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long result = this.get().extract(this.toVariant(resource), amount, transaction);
            transaction.commit();
            return result;
        }
    }

    @Override
    public final long consume(int i, T resource, long amount) {
        if (!this.get().supportsExtraction()) {
            return 0L;
        }
        StorageView<V> view = this.get(i);
        if (view == null) {
            return 0L;
        } else {
            try (Transaction transaction = Transaction.openOuter()) {
                long result = view.extract(this.toVariant(resource), amount, transaction);
                transaction.commit();
                return result;
            }
        }
    }

    @Override
    public final long simAccept(int i, T resource, long amount) {
        if (!this.get().supportsInsertion()) {
            return 0L;
        }
        StorageView<V> view = this.get(i);
        if (!(view instanceof Storage<?> s)) {
            return 0L;
        } else {
            try {
                @SuppressWarnings("unchecked")
                Storage<V> storage = (Storage<V>) s;
                return StorageUtil.simulateInsert(storage, this.toVariant(resource), amount, null);
            } catch (ClassCastException e) {
                return 0L;
            }
        }
    }

    @Override
    public final long accept(int i, T resource, long amount) {
        if (!this.get().supportsInsertion()) {
            return 0L;
        }
        StorageView<V> view = this.get(i);
        if (!(view instanceof Storage<?> s)) {
            return 0L;
        } else {
            try {
                @SuppressWarnings("unchecked")
                Storage<V> storage = (Storage<V>) s;
                try (Transaction transaction = Transaction.openOuter()) {
                    long result = storage.insert(this.toVariant(resource), amount, transaction);
                    transaction.commit();
                    return result;
                }
            } catch (ClassCastException e) {
                return 0L;
            }
        }
    }

    @Override
    public final long accept(T resource, long amount) {
        if (!this.get().supportsInsertion()) {
            return 0L;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long result = this.get().insert(this.toVariant(resource), amount, transaction);
            transaction.commit();
            return result;
        }
    }

    @Override
    public final long capacityFor(int i, T resource) {
        StorageView<V> view = this.get(i);
        if (view == null) {
            return 0L;
        } else if (view.isResourceBlank() || this.matches(view.getResource(), resource)) {
            return view.getCapacity();
        } else {
            return 0L;
        }
    }

    @Override
    public final long capacityFor(T resource) {
        if (!this.get().supportsInsertion()) {
            return 0L;
        }
        long result = 0L;
        for (StorageView<V> view : this.get()) {
            if (this.matches(view.getResource(), resource) || view.isResourceBlank()) {
                result += view.getCapacity();
            }
        }
        return result;
    }

    @Override
    public final long amountFor(int i, T resource) {
        StorageView<V> view = this.get(i);
        if (view == null || !this.matches(view.getResource(), resource)) {
            return 0L;
        } else {
            return view.getAmount();
        }
    }

    @Override
    public final long amountFor(T resource) {
        long result = 0L;
        for (StorageView<V> view : this.get()) {
            if (this.matches(view.getResource(), resource)) {
                result += view.getAmount();
            }
        }
        return result;
    }
}
