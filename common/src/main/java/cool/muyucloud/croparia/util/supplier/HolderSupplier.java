package cool.muyucloud.croparia.util.supplier;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.DeferredSupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * A supplier that provides a value from a registry with given ID and registration methods.
 */
public class HolderSupplier<T> implements DeferredSupplier<T> {
    public static <T> HolderSupplier<T> of(Supplier<T> value, Identifier location, Registry<? super T> registry) {
        return new HolderSupplier<>(value, ResourceKey.create(registry.key(), location));
    }

    public static <S, T extends S> HolderSupplier<T> of(Supplier<T> value, Identifier location, ResourceKey<Registry<S>> registry) {
        return new HolderSupplier<>(value, ResourceKey.create(registry, location));
    }

    @NonNull
    private final ResourceKey<? super T> key;
    @NonNull
    private final Registry<? super T> registry;
    @NonNull
    private final LazySupplier<T> value;

    @SuppressWarnings("unchecked")
    public HolderSupplier(@NonNull Supplier<T> value, @NonNull ResourceKey<? super T> key) {
        this.value = LazySupplier.of(value);
        this.key = key;
        this.registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(this.getRegistryId());
        if (this.registry == null) throw new IllegalArgumentException("Invalid registry id: " + this.getRegistryId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull ResourceKey<T> getKey() {
        return (ResourceKey<T>) this.key;
    }

    @SuppressWarnings("unchecked")
    public <S> ResourceKey<S> adaptKey() {
        return (ResourceKey<S>) this.key;
    }

    public @NonNull Registry<? super T> getRegistry() {
        return this.registry;
    }

    @Override
    public Identifier getRegistryId() {
        return getKey().registry();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourceKey<Registry<T>> getRegistryKey() {
        return (ResourceKey<Registry<T>>) this.getRegistry().key();
    }

    @Override
    public Identifier getId() {
        return this.getKey().identifier();
    }

    @Override
    public boolean isPresent() {
        return this.getRegistry().containsKey(this.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) this.getRegistry().getValue(this.getId());
    }

    public void register() {
        DeferredRegister<T> register = DeferredRegister.create(this.getId().getNamespace(), getRegistryKey());
        register.register(this.getId().getPath(), this.value);
        register.register();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean tryRegister() {
        try {
            this.register();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
