package cool.muyucloud.croparia.util;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Vec2iTest {
    @Test
    void compareToSortsByXThenZ() {
        Vec2i a = Vec2i.of(1, 5);
        Vec2i b = Vec2i.of(2, 0);
        Vec2i c = Vec2i.of(2, 3);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(c) < 0);
    }

    @Test
    void ofVec3iDropsY() {
        Vec2i vec = Vec2i.of(new Vec3i(7, 99, -2));
        assertEquals(7, vec.x());
        assertEquals(-2, vec.z());
    }

    @Test
    void toVec3iAndListWork() {
        Vec2i vec = Vec2i.of(3, 4);
        assertEquals(new Vec3i(3, 10, 4), vec.toVec3i(10));
        assertEquals(List.of(3, 4), vec.toList());
    }

    @Test
    void codecRoundTrip() {
        Vec2i input = Vec2i.of(8, -6);
        var encoded = Vec2i.CODEC.encodeStart(JsonOps.INSTANCE, input).getOrThrow();
        Vec2i decoded = Vec2i.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(input, decoded);
    }
}
