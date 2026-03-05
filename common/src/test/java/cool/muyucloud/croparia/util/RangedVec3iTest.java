package cool.muyucloud.croparia.util;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangedVec3iTest {
    @Test
    void constructorClampsToBounds() {
        RangedVec3i vec = new RangedVec3i(0, 0, 0, 5, 5, 5, 9, -1, 3);
        assertEquals(5, vec.getX());
        assertEquals(0, vec.getY());
        assertEquals(3, vec.getZ());
    }

    @Test
    void volumeAndInsideChecksWork() {
        RangedVec3i vec = new RangedVec3i(1, 2, 3, 3, 4, 5, 1, 2, 3);
        assertEquals(27, vec.volume());
        assertTrue(vec.isInside(2, 3, 4));
        assertFalse(vec.isInside(0, 3, 4));
        assertTrue(vec.testX(1));
        assertTrue(vec.testY(4));
        assertTrue(vec.testZ(5));
    }

    @Test
    void rebuildAndRelativeZeroReturnSameInstance() {
        RangedVec3i vec = RangedVec3i.maxBounds(4, 4, 4, 2, 2, 2);
        assertSame(vec, vec.rebuild(2, 2, 2));
        assertSame(vec, vec.relative(Direction.Axis.X, 0));
        assertSame(vec, vec.relative(Direction.NORTH, 0));
        assertSame(vec, vec.multiply(0));
    }

    @Test
    void offsetAndDirectionalMovesClamp() {
        RangedVec3i vec = RangedVec3i.maxBounds(2, 2, 2, 1, 1, 1);
        RangedVec3i moved = vec.relative(Direction.EAST, 10).above(10).south(10);
        assertEquals(2, moved.getX());
        assertEquals(2, moved.getY());
        assertEquals(2, moved.getZ());
    }
}
