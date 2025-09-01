package cool.muyucloud.croparia.util;

import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @apiNote DO NOT fill functional param with method reference where class you do not want to load on server side is explicitly called. <br/>
 * For example, do NOT use {@code SidedRef.ofServer(SomeClass::new)} if {@code SomeClass} is only present on server side.<br/>
 * Use lambda expression instead: {@code SidedRef.ofServer(() -> new SomeClass())}<br/>
 * If you meet with "Lambda can be replaced with method reference" warning, please ignore it with {@code // noinspection Convert2MethodRef}.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class SidedRef<T> {
    public static boolean ifSideOrElse(Env side, Runnable run, Runnable orElse) {
        if (Platform.getEnvironment() == side) {
            run.run();
            return true;
        } else {
            orElse.run();
            return false;
        }
    }

    public static boolean ifSide(Env side, Runnable run) {
        return ifSideOrElse(side, run, () -> {
        });
    }

    public static void ifServerOrElse(Runnable run, Runnable orElse) {
        ifSideOrElse(Env.SERVER, run, orElse);
    }

    public static boolean ifServer(Runnable run) {
        return ifSide(Env.SERVER, run);
    }

    public static void ifClientOrElse(Runnable run, Runnable orElse) {
        ifSideOrElse(Env.CLIENT, run, orElse);
    }

    public static boolean ifClient(Runnable run) {
        return ifSide(Env.CLIENT, run);
    }

    public static <T> SidedRef<T> ofClient(Supplier<T> supplier) {
        return new SidedRef<>(LazySupplier.of(supplier), Env.CLIENT);
    }

    public static <T> SidedRef<T> ofServer(Supplier<T> supplier) {
        return new SidedRef<>(LazySupplier.of(supplier), Env.SERVER);
    }

    private final LazySupplier<T> value;
    private final Env side;

    public SidedRef(LazySupplier<T> supplier, Env side) {
        this.value = supplier;
        this.side = side;
    }

    public boolean use(Consumer<T> consumer) {
        return ifSide(this.side, () -> consumer.accept(this.value.get()));
    }

    public boolean useOrElse(Consumer<T> consumer, Runnable orElse) {
        return ifSideOrElse(this.side, () -> consumer.accept(this.value.get()), orElse);
    }
}
