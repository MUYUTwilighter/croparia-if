package cool.muyucloud.croparia.util;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifUtilTest {
    @Test
    void formatIdAllNullAndCastUnsafeWork() {
        Identifier id = Identifier.fromNamespaceAndPath("croparia", "seed");
        Identifier formatted = CifUtil.formatId("x_%s", id);
        assertEquals("croparia:x_seed", formatted.toString());

        assertTrue(CifUtil.allNull(null, null, null));
        assertFalse(CifUtil.allNull(null, "x", null));

        Object value = "abc";
        assertEquals("abc", CifUtil.<String>castUnsafe(value));
    }

    @Test
    void toIntSafeClampsLongRange() {
        assertEquals(Integer.MIN_VALUE, CifUtil.toIntSafe((long) Integer.MIN_VALUE - 1L));
        assertEquals(Integer.MAX_VALUE, CifUtil.toIntSafe((long) Integer.MAX_VALUE + 1L));
    }

    @Test
    void toIntSafeKeepsValueInRange() {
        assertEquals(123, CifUtil.toIntSafe(123L));
        assertEquals(-456, CifUtil.toIntSafe(-456L));
    }

    @Test
    void toIntSafeClampsFloatRange() {
        assertEquals(Integer.MIN_VALUE, CifUtil.toIntSafe((float) Integer.MIN_VALUE - 1000f));
        assertEquals(Integer.MAX_VALUE, CifUtil.toIntSafe((float) Integer.MAX_VALUE + 1000f));
    }

    @Test
    void toIntSafeClampsDoubleRange() {
        assertEquals(Integer.MIN_VALUE, CifUtil.toIntSafe((double) Integer.MIN_VALUE - 1000d));
        assertEquals(Integer.MAX_VALUE, CifUtil.toIntSafe((double) Integer.MAX_VALUE + 1000d));
    }
}
