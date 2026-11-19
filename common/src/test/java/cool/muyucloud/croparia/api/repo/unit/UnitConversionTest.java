package cool.muyucloud.croparia.api.repo.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitConversionTest {
    @Test
    void fluidUnitsShareHalfDropletBase() {
        assertSame(Unit.HALF_DROPLET, Unit.HALF_DROPLET.baseUnit());
        assertSame(Unit.HALF_DROPLET, Unit.DROPLET.baseUnit());
        assertSame(Unit.HALF_DROPLET, Unit.MILL_BUCKET.baseUnit());
        assertSame(Unit.COUNT, Unit.COUNT.baseUnit());
    }

    @Test
    void bucketFractionsRetainHalfDropletPrecision() {
        assertEquals(2, Unit.DROPLET.toBase(1));
        assertEquals(162, Unit.MILL_BUCKET.toBase(1));
        assertEquals(162_000, Unit.DROPLET.toBase(81_000));
        assertEquals(162_000, Unit.MILL_BUCKET.toBase(1_000));
        assertEquals(new Unit.Result(81_000, 0), Unit.HALF_DROPLET.to(162_000, Unit.DROPLET));
        assertEquals(new Unit.Result(1_000, 0), Unit.HALF_DROPLET.to(162_000, Unit.MILL_BUCKET));

        long sixteenthBucket = 10_125;
        assertEquals(new Unit.Result(5_062, 1), Unit.HALF_DROPLET.to(sixteenthBucket, Unit.DROPLET));
        assertEquals(new Unit.Result(62, 81), Unit.HALF_DROPLET.to(sixteenthBucket, Unit.MILL_BUCKET));
        assertEquals(new Unit.Result(81, 0), Unit.MILL_BUCKET.to(1, Unit.DROPLET));
        assertEquals(new Unit.Result(1, 2), Unit.DROPLET.to(82, Unit.MILL_BUCKET));
    }

    @Test
    void conversionsConserveBaseAmountsAndRemainders() {
        FluidUnit[] units = {Unit.HALF_DROPLET, Unit.DROPLET, Unit.MILL_BUCKET};
        long[] amounts = {0, 1, 2, 81, 1_000, 10_125, 81_000, 162_000};
        for (FluidUnit source : units) {
            for (FluidUnit target : units) {
                for (long amount : amounts) {
                    Unit.Result converted = source.to(amount, target);
                    assertEquals(converted, target.from(amount, source));
                    assertEquals(source.toBase(amount), target.toBase(converted.amount()) + converted.remainder());
                    assertTrue(converted.remainder() >= 0);
                    assertTrue(converted.remainder() < target.toBase(1));
                }
            }
        }
    }

    @Test
    void positiveOverflowClampsBeforeMultiplication() {
        for (FluidUnit unit : new FluidUnit[]{Unit.DROPLET, Unit.MILL_BUCKET}) {
            long scale = unit.toBase(1);
            long threshold = Long.MAX_VALUE / scale;
            long maximumBase = Long.MAX_VALUE - Long.MAX_VALUE % scale;
            assertEquals(Math.multiplyExact(threshold - 1, scale), unit.toBase(threshold - 1));
            assertEquals(maximumBase, unit.toBase(threshold));
            assertEquals(maximumBase, unit.toBase(threshold + 1));
            assertEquals(maximumBase, unit.toBase(Long.MAX_VALUE));
            assertEquals(new Unit.Result(threshold, Long.MAX_VALUE % scale), unit.fromBase(Long.MAX_VALUE));
        }
        assertEquals(Long.MAX_VALUE, Unit.HALF_DROPLET.toBase(Long.MAX_VALUE));
        assertEquals(new Unit.Result(Long.MAX_VALUE, 0), Unit.HALF_DROPLET.fromBase(Long.MAX_VALUE));
    }
}
