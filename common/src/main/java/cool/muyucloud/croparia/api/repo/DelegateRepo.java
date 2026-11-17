package cool.muyucloud.croparia.api.repo;

import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DelegateRepo<T extends TypedResource<?>> implements Repo<T> {
    private final @NotNull Repo<T> delegate;
    private final Set<Integer> lockAccept = new HashSet<>();
    private final Set<Integer> lockConsume = new HashSet<>();

    public DelegateRepo(@NotNull Repo<T> repo) {
        this.delegate = repo;
    }

    public DelegateRepo(@NotNull Repo<T> repo, Collection<Integer> lockAccept, Collection<Integer> lockConsume) {
        this.delegate = repo;
        this.lockAccept.addAll(lockAccept);
        this.lockConsume.addAll(lockConsume);
    }

    public @NotNull Repo<T> get() {
        return delegate;
    }

    @Override
    public int size() {
        return this.get().size();
    }

    @Override
    public TypeToken<T> getType() {
        return this.get().getType();
    }

    @Override
    public boolean isEmpty(int i) {
        return this.get().isEmpty(i);
    }

    @Override
    public boolean isEmpty() {
        return this.get().isEmpty();
    }

    @Override
    public T resourceFor(int i) {
        return this.get().resourceFor(i);
    }

    @Override
    public boolean isConsumeLocked(int i) {
        return this.lockConsume.contains(i) || this.get().isConsumeLocked(i);
    }

    @Override
    public long simConsume(int i, T resource, long amount) {
        if (this.isConsumeLocked(i)) return 0;
        return this.get().simConsume(i, resource, amount);
    }

    @Override
    public long simConsume(int i, long amount) {
        if (this.isConsumeLocked(i)) return 0;
        return this.get().simConsume(i, amount);
    }

    @Override
    public long consume(int i, T resource, long amount) {
        if (this.isConsumeLocked(i)) return 0;
        return this.get().consume(i, resource, amount);
    }

    @Override
    public long consume(int i, long amount) {
        if (this.isConsumeLocked(i)) return 0;
        return this.get().consume(i, amount);
    }

    @Override
    public boolean isAcceptLocked(int i) {
        return this.lockAccept.contains(i) || this.get().isAcceptLocked(i);
    }

    @Override
    public long simAccept(int i, T resource, long amount) {
        if (this.isAcceptLocked(i)) return 0;
        return this.get().simAccept(i, resource, amount);
    }

    @Override
    public long simAccept(int i, long amount) {
        if (this.isAcceptLocked(i)) return 0;
        return this.get().simAccept(i, amount);
    }

    @Override
    public long accept(int i, T resource, long amount) {
        if (this.isAcceptLocked(i)) return 0;
        return this.get().accept(i, resource, amount);
    }

    @Override
    public long accept(int i, long amount) {
        if (this.isAcceptLocked(i)) return 0;
        return this.get().accept(i, amount);
    }

    @Override
    public long capacityFor(int i, T resource) {
        return this.get().capacityFor(i, resource);
    }

    @Override
    public long capacityFor(int i) {
        return this.get().capacityFor(i);
    }

    @Override
    public long capacityFor(T resource) {
        return this.get().capacityFor(resource);
    }

    @Override
    public long amountFor(int i, T resource) {
        return this.get().amountFor(i, resource);
    }

    @Override
    public long amountFor(T resource) {
        return this.get().amountFor(resource);
    }

    @Override
    public long amountFor(int i) {
        return this.get().amountFor(i);
    }

    public DelegateRepo<T> trim() {
        Set<Integer> lockAccept = new HashSet<>();
        Set<Integer> lockConsume = new HashSet<>();
        Repo<?> tmp = this;
        while (tmp instanceof DelegateRepo<?> delegated) {
            lockAccept.addAll(delegated.lockAccept);
            lockConsume.addAll(delegated.lockConsume);
            tmp = delegated.get();
        }
        @SuppressWarnings("unchecked")
        Repo<T> repo = (Repo<T>) tmp;
        return new DelegateRepo<>(repo, lockAccept, lockConsume);
    }
}
