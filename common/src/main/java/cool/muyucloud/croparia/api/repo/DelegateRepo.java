package cool.muyucloud.croparia.api.repo;

import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import org.jetbrains.annotations.NotNull;

public class DelegateRepo<T extends TypedResource<?>> implements Repo<T> {
    private final @NotNull Repo<T> delegate;

    public DelegateRepo(@NotNull Repo<T> repo) {
        this.delegate = repo;
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
    public long simConsume(int i, T resource, long amount) {
        return this.get().simConsume(i, resource, amount);
    }

    @Override
    public long simConsume(T resource, long amount) {
        return this.get().simConsume(resource, amount);
    }

    @Override
    public long simConsume(int i, long amount) {
        return this.get().simConsume(i, amount);
    }

    @Override
    public long consume(int i, T resource, long amount) {
        return this.get().consume(i, resource, amount);
    }

    @Override
    public long consume(T resource, long amount) {
        return this.get().consume(resource, amount);
    }

    @Override
    public long consume(int i, long amount) {
        return this.get().consume(i, amount);
    }

    @Override
    public long simAccept(int i, T resource, long amount) {
        return this.get().simAccept(i, resource, amount);
    }

    @Override
    public long simAccept(int i, long amount) {
        return this.get().simAccept(i, amount);
    }

    @Override
    public long simAccept(T resource, long amount) {
        return this.get().simAccept(resource, amount);
    }

    @Override
    public long accept(int i, T resource, long amount) {
        return this.get().accept(i, resource, amount);
    }

    @Override
    public long accept(int i, long amount) {
        return this.get().accept(i, amount);
    }

    @Override
    public long accept(T resource, long amount) {
        return this.get().accept(resource, amount);
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
}
