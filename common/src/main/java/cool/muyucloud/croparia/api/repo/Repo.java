package cool.muyucloud.croparia.api.repo;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypeTokenAccess;
import cool.muyucloud.croparia.api.resource.TypedResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstraction of resource storage.<br>
 */
@SuppressWarnings("unused")
public interface Repo<T extends TypedResource<?>> extends TypeTokenAccess {
    Logger LOGGER = LogUtils.getLogger();

    /**
     * The amount of resource storage units
     */
    int size();

    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!isEmpty(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    TypeToken<T> getType();

    /**
     * Whether the specified resource storage unit is empty
     */
    boolean isEmpty(int i);

    /**
     * Query the resource type of the specified storage unit
     *
     * @param i The index of the resource storage unit
     * @return The resource stored in the specified resource storage unit
     */
    T resourceFor(int i);

    /**
     * Simulates consuming the specified amount of resource from the total storage.
     *
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return The amount that can be consumed
     */
    default long simConsume(T resource, long amount) {
        long required = amount;
        for (int i = 0; i < size() && amount > 0; i++) {
            amount -= simConsume(i, resource, amount);
        }
        return required - amount;
    }

    default long simConsume(int i, long amount) {
        return this.simConsume(i, this.resourceFor(i), amount);
    }

    /**
     * Simulates consuming the specified amount of resource from the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to consume
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return The amount that can be consumed
     */
    long simConsume(int i, T resource, long amount);

    /**
     * Consumes the specified amount of resource from the total storage.
     *
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return the amount actually consumed
     */
    default long consume(T resource, long amount) {
        long required = amount;
        for (int i = 0; i < size() && amount > 0; i++) {
            amount -= consume(i, resource, amount);
        }
        return required - amount;
    }

    default long consume(int i, long amount) {
        return this.consume(i, this.resourceFor(i), amount);
    }

    /**
     * Consumes the specified amount of resource from the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to consume
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return The amount actually consumed
     */
    long consume(int i, T resource, long amount);


    /**
     * Simulates accepting the specified amount of resource into the total storage.
     *
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return The amount that can be accepted
     */
    default long simAccept(T resource, long amount) {
        long required = amount;
        for (int i = 0; i < size() && amount > 0; i++) {
            amount -= simAccept(i, resource, amount);
        }
        return required - amount;
    }

    default long simAccept(int i, long amount) {
        return this.simAccept(i, this.resourceFor(i), amount);
    }

    /**
     * Simulates accepting the specified amount of resource into the specified resource storage.
     *
     * @param i        The index of the resource storage unit to accept
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return The amount that can be accepted
     */
    long simAccept(int i, T resource, long amount);

    /**
     * Accepts the specified amount of resource into the total storage.
     *
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return the amount actually accepted
     */
    default long accept(T resource, long amount) {
        long required = amount;
        for (int i = 0; i < size() && amount > 0; i++) {
            amount -= accept(i, resource, amount);
        }
        return required - amount;
    }

    default long accept(int i, long amount) {
        return this.accept(i, this.resourceFor(i), amount);
    }

    /**
     * Accepts the specified amount of resource into the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to accept
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return The amount actually accepted
     */
    long accept(int i, T resource, long amount);

    /**
     * Calculates the capacity for the specified resource in the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to check
     * @param resource The resource to check
     * @return The capacity for the specified resource
     * @apiNote This is not the room left, but the total capacity for the specified resource.
     */
    long capacityFor(int i, T resource);

    /**
     * Calculates the total capacity for the specified resource across all resource storage units.
     *
     * @param resource The resource to check
     * @return The total capacity for the specified resource
     * @apiNote This is not the room left, but the total capacity for the specified resource.
     */
    default long capacityFor(T resource) {
        long amount = 0;
        for (int i = 0; i < size(); i++) {
            amount += capacityFor(i, resource);
        }
        return amount;
    }

    default long capacityFor(int i) {
        return this.capacityFor(i, this.resourceFor(i));
    }

    /**
     * Calculates the amount of resource in the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to check
     * @param resource The resource to check
     * @return The amount of resource
     */
    long amountFor(int i, T resource);

    /**
     * Calculates the total amount of resource across all resource storage units.
     *
     * @param resource The resource to check
     * @return The total amount of resource
     */
    default long amountFor(T resource) {
        long amount = 0;
        for (int i = 0; i < size(); i++) {
            amount += amountFor(i, resource);
        }
        return amount;
    }

    /**
     * Calculates the amount of whatever resource is in the specified resource storage unit.
     *
     * @param i The index of the resource storage unit to check
     * @return The amount of resource
     */
    default long amountFor(int i) {
        return this.amountFor(i, this.resourceFor(i));
    }

    default @NotNull AcceptOnlyRepo<T> asAcceptOnly() {
        return new AcceptOnlyRepo<>(this);
    }

    default @NotNull ConsumeOnlyRepo<T> asConsumeOnly() {
        return new ConsumeOnlyRepo<>(this);
    }

    default @NotNull LockedRepo<T> asLocked(Integer @NotNull ... idx) {
        return new LockedRepo<>(this, Arrays.stream(idx).collect(Collectors.toUnmodifiableSet()));
    }

    class AcceptOnlyRepo<T extends TypedResource<?>> extends DelegateRepo<T> {
        public AcceptOnlyRepo(@NotNull Repo<T> repo) {
            super(repo);
        }

        @Override
        public long simConsume(int i, T resource, long amount) {
            return 0;
        }

        @Override
        public long simConsume(T resource, long amount) {
            return 0;
        }

        @Override
        public long simConsume(int i, long amount) {
            return 0;
        }

        @Override
        public long consume(int i, T resource, long amount) {
            return 0;
        }

        @Override
        public long consume(T resource, long amount) {
            return 0;
        }

        @Override
        public long consume(int i, long amount) {
            return 0;
        }
    }

    class ConsumeOnlyRepo<T extends TypedResource<?>> extends DelegateRepo<T> {
        public ConsumeOnlyRepo(@NotNull Repo<T> repo) {
            super(repo);
        }

        @Override
        public long simAccept(int i, T resource, long amount) {
            return 0;
        }

        @Override
        public long simAccept(int i, long amount) {
            return 0;
        }

        @Override
        public long simAccept(T resource, long amount) {
            return 0;
        }

        @Override
        public long accept(int i, T resource, long amount) {
            return 0;
        }

        @Override
        public long accept(int i, long amount) {
            return 0;
        }

        @Override
        public long accept(T resource, long amount) {
            return 0;
        }
    }

    class LockedRepo<T extends TypedResource<?>> extends DelegateRepo<T> {
        private final @NotNull Set<Integer> locked;

        public LockedRepo(@NotNull Repo<T> repo, @NotNull Set<Integer> locked) {
            super(repo);
            this.locked = locked;
        }

        public boolean isLocked(int i) {
            return this.locked.contains(i);
        }

        @Override
        public long simConsume(int i, T resource, long amount) {
            if (this.isLocked(i)) return 0;
            return super.simConsume(i, resource, amount);
        }

        @Override
        public long simConsume(int i, long amount) {
            if (this.isLocked(i)) return 0;
            return super.simConsume(i, amount);
        }

        @Override
        public long consume(int i, T resource, long amount) {
            if (this.isLocked(i)) return 0;
            return super.consume(i, resource, amount);
        }

        @Override
        public long consume(int i, long amount) {
            if (this.isLocked(i)) return 0;
            return super.consume(i, amount);
        }

        @Override
        public long simAccept(int i, T resource, long amount) {
            if (this.isLocked(i)) return 0;
            return super.simAccept(i, resource, amount);
        }

        @Override
        public long simAccept(int i, long amount) {
            if (this.isLocked(i)) return 0;
            return super.simAccept(i, amount);
        }

        @Override
        public long accept(int i, T resource, long amount) {
            if (this.isLocked(i)) return 0;
            return super.accept(i, resource, amount);
        }

        @Override
        public long accept(int i, long amount) {
            if (this.isLocked(i)) return 0;
            return super.accept(i, amount);
        }
    }
}
