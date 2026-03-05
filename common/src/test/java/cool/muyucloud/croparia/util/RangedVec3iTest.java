package cool.muyucloud.croparia.util;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void invalidBoundsThrowAndAxisMovesWork() {
        assertThrows(IllegalArgumentException.class, () -> new RangedVec3i(2, 0, 0, 1, 1, 1, 0, 0, 0));

        RangedVec3i vec = RangedVec3i.maxBounds(3, 3, 3, 1, 1, 1);
        assertEquals(3, vec.relative(Direction.Axis.X, 5).getX());
        assertEquals(0, vec.relative(Direction.Axis.Y, -5).getY());
        assertEquals(3, vec.relative(Direction.Axis.Z, 5).getZ());
    }

    @Test
    void minMaxRebuildAndCrossKeepBoundsAndHash() {
        RangedVec3i vec = RangedVec3i.maxBounds(4, 4, 4, 2, 2, 2);
        RangedVec3i tuned = vec.min(1, 1, 1).max(3, 3, 3).rebuild(new Vec3i(9, -9, 2));
        assertEquals(1, tuned.getMinX());
        assertEquals(3, tuned.getMaxY());
        assertEquals(3, tuned.getX());
        assertEquals(1, tuned.getY());
        assertEquals(2, tuned.getZ());

        RangedVec3i cross = tuned.cross(new Vec3i(0, 1, 0));
        assertTrue(cross.isInside(cross.getX(), cross.getY(), cross.getZ()));
        assertEquals(tuned.getMinX(), cross.getX());
        assertEquals(tuned.getMinY(), cross.getY());
        assertEquals(tuned.getMaxZ(), cross.getZ());

        assertEquals(tuned, tuned.rebuild(tuned));
        assertEquals(tuned.hashCode(), tuned.rebuild(tuned).hashCode());
        assertNotEquals(tuned, vec);
    }

    @Test
    void directionalShorthandsAndDeltaHelpersWork() {
        RangedVec3i vec = RangedVec3i.maxBounds(5, 5, 5, 2, 2, 2);

        assertEquals(3, vec.above().getY());
        assertEquals(4, vec.above(2).getY());
        assertEquals(1, vec.below().getY());
        assertEquals(0, vec.below(10).getY());

        assertEquals(1, vec.north().getZ());
        assertEquals(0, vec.north(10).getZ());
        assertEquals(3, vec.south().getZ());
        assertEquals(5, vec.south(10).getZ());

        assertEquals(1, vec.west().getX());
        assertEquals(0, vec.west(10).getX());
        assertEquals(3, vec.east().getX());
        assertEquals(5, vec.east(10).getX());

        assertEquals(5, vec.dx(10).getX());
        assertEquals(0, vec.dy(-10).getY());
        assertEquals(5, vec.dz(10).getZ());
    }

    @Test
    void minMaxAxisSpecificAndFactoryHelpersWork() {
        RangedVec3i vec = RangedVec3i.maxBounds(10, 10, 10, 5, 5, 5);
        RangedVec3i tuned = vec.minX(2).minY(3).minZ(4).maxX(8).maxY(9).maxZ(7);
        assertEquals(2, tuned.getMinX());
        assertEquals(3, tuned.getMinY());
        assertEquals(4, tuned.getMinZ());
        assertEquals(8, tuned.getMaxX());
        assertEquals(9, tuned.getMaxY());
        assertEquals(7, tuned.getMaxZ());

        RangedVec3i byBounds = RangedVec3i.bounds(1, 2, 3, 2, 2, 2);
        assertEquals(1, byBounds.getMinX());
        assertEquals(3, byBounds.getMaxX());
        assertEquals(2, byBounds.getMinY());
        assertEquals(4, byBounds.getMaxY());
        assertEquals(3, byBounds.getMinZ());
        assertEquals(5, byBounds.getMaxZ());
    }
}
