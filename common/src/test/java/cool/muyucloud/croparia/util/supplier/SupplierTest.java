package cool.muyucloud.croparia.util.supplier;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierTest {
    @Test
    void lazySupplierLoadsOnceAndCachesValue() {
        AtomicInteger calls = new AtomicInteger(0);
        LazySupplier<Integer> supplier = LazySupplier.of(calls::incrementAndGet);

        assertFalse(supplier.isLoaded());
        assertEquals(1, supplier.get());
        assertTrue(supplier.isLoaded());
        assertEquals(1, supplier.get());
        assertEquals(1, calls.get());
    }

    @Test
    void lazySupplierMapUsesCachedSourceValue() {
        AtomicInteger calls = new AtomicInteger(0);
        LazySupplier<Integer> supplier = LazySupplier.of(calls::incrementAndGet);
        LazySupplier<String> mapped = supplier.map(v -> "v=" + v);

        assertEquals("v=1", mapped.get());
        assertEquals("v=1", mapped.get());
        assertEquals(1, calls.get());
    }

    @Test
    void semiSupplierRefreshForcesRecompute() {
        AtomicInteger calls = new AtomicInteger(0);
        SemiSupplier<Integer> supplier = SemiSupplier.of(calls::incrementAndGet);

        assertEquals(1, supplier.get());
        assertEquals(1, supplier.get());
        supplier.refresh();
        assertEquals(2, supplier.get());
        assertEquals(2, calls.get());
    }

    @Test
    void onLoadSupplierRecomputesWhenDataLoadTicks() {
        long original = OnLoadSupplier.LAST_DATA_LOAD;
        try {
            AtomicInteger calls = new AtomicInteger(0);
            OnLoadSupplier<Integer> supplier = OnLoadSupplier.of(calls::incrementAndGet);

            assertEquals(1, supplier.get());
            assertEquals(1, supplier.get());
            assertEquals(1, calls.get());

            OnLoadSupplier.LAST_DATA_LOAD = supplier.getLastCreate();
            assertEquals(2, supplier.get());
            assertEquals(2, calls.get());
        } finally {
            OnLoadSupplier.LAST_DATA_LOAD = original;
        }
    }

    @Test
    void onLoadSupplierMapRecomputesAfterDataLoadTick() {
        long original = OnLoadSupplier.LAST_DATA_LOAD;
        try {
            AtomicInteger calls = new AtomicInteger(0);
            OnLoadSupplier<Integer> base = OnLoadSupplier.of(() -> calls.incrementAndGet());
            OnLoadSupplier<String> mapped = base.map(v -> "v" + v);

            assertEquals("v1", mapped.get());
            assertEquals("v1", mapped.get());
            assertEquals(1, calls.get());

            OnLoadSupplier.LAST_DATA_LOAD = mapped.getLastCreate();
            assertEquals("v2", mapped.get());
            assertEquals(2, calls.get());
        } finally {
            OnLoadSupplier.LAST_DATA_LOAD = original;
        }
    }

    @Test
    void mappableHelpersPreserveAndTransformValues() {
        Mappable<Integer> source = Mappable.of(() -> 3);
        Mappable<String> mapped = source.map(v -> "n=" + v);
        assertEquals("n=3", mapped.get());
        assertEquals("fallback", Mappable.<String>of(() -> null).getOr("fallback"));
    }

    @Test
    void lazySupplierOfReturnsSameInstanceForExactLazySupplier() {
        LazySupplier<Integer> base = new LazySupplier<>(() -> 7);
        LazySupplier<Integer> wrapped = LazySupplier.of(base);
        assertSame(base, wrapped);
    }

    @Test
    void lazyAndSemiEmptyReturnNullByDefault() {
        assertNull(LazySupplier.empty().get());
        assertNull(SemiSupplier.empty().get());
    }

    @Test
    void semiSupplierOfReturnsSameInstanceWhenAlreadySemiSupplier() {
        SemiSupplier<Integer> base = SemiSupplier.of(() -> 1);
        assertSame(base, SemiSupplier.of(base));
    }

    @Test
    void semiSupplierMapProducesMappedSemiSupplier() {
        AtomicInteger calls = new AtomicInteger(0);
        SemiSupplier<Integer> base = SemiSupplier.of(() -> calls.incrementAndGet());
        SemiSupplier<String> mapped = base.map(v -> "s" + v);

        assertEquals("s1", mapped.get());
        assertEquals("s1", mapped.get());
        mapped.refresh();
        assertEquals("s2", mapped.get());
    }

    @Test
    void mappableOfUsesDifferentPathsForMappableAndPlainSupplier() {
        AtomicInteger calls = new AtomicInteger(0);
        Mappable<Integer> mappableSource = Mappable.of(() -> {
            calls.incrementAndGet();
            return 2;
        });
        Mappable<String> mappedFromMappable = Mappable.of(mappableSource, v -> "m" + v);
        assertEquals("m2", mappedFromMappable.get());

        java.util.function.Supplier<Integer> plain = () -> 3;
        Mappable<String> mappedFromPlain = Mappable.of(plain, v -> "p" + v);
        assertEquals("p3", mappedFromPlain.get());
        assertEquals(1, calls.get());
    }
}
