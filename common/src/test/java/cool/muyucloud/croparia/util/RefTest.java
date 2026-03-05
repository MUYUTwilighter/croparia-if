package cool.muyucloud.croparia.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefTest {
    @Test
    void setAndMapUpdateValue() {
        Ref<Integer> ref = Ref.of(1);
        ref.set(2).map(v -> v + 1);
        assertEquals(3, ref.get());
    }

    @Test
    void onChangedTriggersOnlyWhenValueActuallyChanges() {
        Ref<String> ref = Ref.of("a");
        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<String> oldRef = new AtomicReference<>();
        AtomicReference<String> newRef = new AtomicReference<>();
        ref.onChanged((oldValue, newValue) -> {
            count.incrementAndGet();
            oldRef.set(oldValue);
            newRef.set(newValue);
        });

        ref.set("a");
        assertEquals(0, count.get());

        ref.set("b");
        assertEquals(1, count.get());
        assertEquals("a", oldRef.get());
        assertEquals("b", newRef.get());
    }

    @Test
    void mapAndCompareReflectsEquality() {
        Ref<Integer> ref = Ref.of(3);
        assertTrue(ref.mapAndCompare(v -> 3));
        assertFalse(ref.mapAndCompare(v -> 4));
    }

    @Test
    void mapAndCompareHandlesNullValues() {
        Ref<String> ref = Ref.of("x");
        assertFalse(ref.mapAndCompare(v -> null));
    }

    @Test
    void optionalReflectsNullability() {
        Ref<String> ref = new Ref<>();
        assertTrue(ref.optional().isEmpty());
        ref.set("x");
        assertEquals("x", ref.optional().orElseThrow());
    }
}
