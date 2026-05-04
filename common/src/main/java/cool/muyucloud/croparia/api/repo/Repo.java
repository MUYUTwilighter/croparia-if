package cool.muyucloud.croparia.api.repo;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypeTokenAccess;
import cool.muyucloud.croparia.api.resource.TypedResource;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Abstraction of resource storage.
 * <p>
 * A repo is an indexed collection of resource storage units. Implementations define the actual storage backend, while
 * default methods provide common whole-repo operations by iterating over every unit.
 * </p>
 * <p>
 * Lock methods are view-level filters. A locked accept unit rejects {@link #simAccept(int, TypedResource, long)} and
 * {@link #accept(int, TypedResource, long)} calls; a locked consume unit rejects
 * {@link #simConsume(int, TypedResource, long)} and {@link #consume(int, TypedResource, long)} calls. Capacity and amount
 * queries remain raw storage queries and should not be changed by locks.
 * </p>
 */
@SuppressWarnings("unused")
public interface Repo<T extends TypedResource<?>> extends TypeTokenAccess {
    Logger LOGGER = LogUtils.getLogger();

    /**
     * The amount of resource storage units.
     *
     * @return The number of addressable storage units in this repo.
     */
    int size();

    /**
     * Checks whether all storage units are empty.
     *
     * @return {@code true} if every storage unit is empty.
     */
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
     * Checks whether the specified resource storage unit is empty.
     *
     * @param i The index of the resource storage unit.
     * @return {@code true} if the specified storage unit is empty.
     */
    boolean isEmpty(int i);

    /**
     * Queries the resource type of the specified storage unit.
     * <p>
     * Empty storage units may return an implementation-defined empty resource value.
     * </p>
     *
     * @param i The index of the resource storage unit.
     * @return The resource stored in the specified resource storage unit.
     */
    T resourceFor(int i);

    /**
     * Simulates consuming the specified amount of resource from the total storage.
     *
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return The amount that can be consumed
     * @apiNote Consume-locked units should contribute {@code 0}.
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
     * Checks whether the specified storage unit is locked for consuming.
     *
     * @param i The index of the resource storage unit.
     * @return {@code true} if consume operations should be rejected for this unit.
     * @apiNote This lock affects consume operations only; it does not affect amount or capacity queries.
     */
    default boolean isConsumeLocked(int i) {
        return false;
    }

    /**
     * Simulates consuming the specified amount of resource from the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to consume
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return The amount that can be consumed
     * @apiNote Implementations or wrappers should return {@code 0} when {@link #isConsumeLocked(int)} is {@code true}.
     */
    long simConsume(int i, T resource, long amount);

    /**
     * Consumes the specified amount of resource from the total storage.
     *
     * @param resource The resource to consume
     * @param amount   The amount to consume
     * @return the amount actually consumed
     * @apiNote Consume-locked units should contribute {@code 0}.
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
     * @apiNote Implementations or wrappers should return {@code 0} when {@link #isConsumeLocked(int)} is {@code true}.
     */
    long consume(int i, T resource, long amount);


    /**
     * Simulates accepting the specified amount of resource into the total storage.
     *
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return The amount that can be accepted
     * @apiNote Accept-locked units should contribute {@code 0}.
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
     * Checks whether the specified storage unit is locked for accepting.
     *
     * @param i The index of the resource storage unit.
     * @return {@code true} if accept operations should be rejected for this unit.
     * @apiNote This lock affects accept operations only; it does not affect amount or capacity queries.
     */
    default boolean isAcceptLocked(int i) {
        return false;
    }

    /**
     * Simulates accepting the specified amount of resource into the specified resource storage.
     *
     * @param i        The index of the resource storage unit to accept
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return The amount that can be accepted
     * @apiNote Implementations or wrappers should return {@code 0} when {@link #isAcceptLocked(int)} is {@code true}.
     */
    long simAccept(int i, T resource, long amount);

    /**
     * Accepts the specified amount of resource into the total storage.
     *
     * @param resource The resource to accept
     * @param amount   The amount to accept
     * @return the amount actually accepted
     * @apiNote Accept-locked units should contribute {@code 0}.
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
     * @apiNote Implementations or wrappers should return {@code 0} when {@link #isAcceptLocked(int)} is {@code true}.
     */
    long accept(int i, T resource, long amount);

    /**
     * Calculates the capacity for the specified resource in the specified resource storage unit.
     *
     * @param i        The index of the resource storage unit to check
     * @param resource The resource to check
     * @return The capacity for the specified resource
     * @apiNote This is not the room left, but the total capacity for the specified resource. Locks should not affect this
     * query; use {@link #isAcceptLocked(int)} when the caller needs to know whether insertion is allowed.
     */
    long capacityFor(int i, T resource);

    /**
     * Calculates the total capacity for the specified resource across all resource storage units.
     *
     * @param resource The resource to check
     * @return The total capacity for the specified resource
     * @apiNote This is not the room left, but the total capacity for the specified resource. Locks should not affect this
     * query.
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

    /**
     * Creates a delegate view that rejects accept operations for the specified storage units.
     *
     * @param idx The storage unit indexes to lock for accepting.
     * @return A delegate repo with the accept locks applied.
     */
    default DelegateRepo<T> lockAccept(Integer... idx) {
        return new DelegateRepo<>(this, List.of(idx), Collections.emptySet());
    }

    /**
     * Creates a delegate view that rejects accept operations for all storage units.
     *
     * @return A delegate repo with every storage unit locked for accepting.
     */
    default DelegateRepo<T> lockAccept() {
        return new DelegateRepo<>(this, this.allIndexes(), Collections.emptySet());
    }

    /**
     * Creates a delegate view that rejects consume operations for the specified storage units.
     *
     * @param idx The storage unit indexes to lock for consuming.
     * @return A delegate repo with the consume locks applied.
     */
    default DelegateRepo<T> lockConsume(Integer... idx) {
        return new DelegateRepo<>(this, Collections.emptySet(), List.of(idx));
    }

    /**
     * Creates a delegate view that rejects consume operations for all storage units.
     *
     * @return A delegate repo with every storage unit locked for consuming.
     */
    default DelegateRepo<T> lockConsume() {
        return new DelegateRepo<>(this, Collections.emptySet(), this.allIndexes());
    }

    /**
     * Creates a delegate view that rejects both accept and consume operations for the specified storage units.
     *
     * @param idx The storage unit indexes to lock for both accepting and consuming.
     * @return A delegate repo with both accept and consume locks applied.
     */
    default DelegateRepo<T> lock(Integer... idx) {
        return new DelegateRepo<>(this, List.of(idx), List.of(idx));
    }

    /**
     * Creates a delegate view that rejects both accept and consume operations for all storage units.
     *
     * @return A delegate repo with every storage unit locked for both accepting and consuming.
     */
    default DelegateRepo<T> lock() {
        List<Integer> indexes = this.allIndexes();
        return new DelegateRepo<>(this, indexes, indexes);
    }

    /**
     * Collects all storage unit indexes in this repo.
     *
     * @return A list containing every valid storage unit index from {@code 0} inclusive to {@link #size()} exclusive.
     */
    private List<Integer> allIndexes() {
        return IntStream.range(0, this.size()).boxed().toList();
    }
}
