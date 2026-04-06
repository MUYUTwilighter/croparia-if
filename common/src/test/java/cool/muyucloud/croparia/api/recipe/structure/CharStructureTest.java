package cool.muyucloud.croparia.api.recipe.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CharStructureTest {
    @Test
    void char2dValidatesShapeAndSupportsTransforms() {
        assertThrows(IllegalArgumentException.class, () -> new Char2D(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Char2D(List.of("ab", "a")));

        Char2D layer = new Char2D(List.of("ab", "cd", "ef"));
        assertEquals(2, layer.xSize());
        assertEquals(3, layer.zSize());
        assertEquals('a', layer.get(0, 0));
        assertEquals('f', layer.get(1, 2));
        assertTrue(layer.contains('c'));
        assertEquals(1, layer.count('c'));
        assertTrue(layer.find('e').isPresent());
        assertTrue(layer.find('x').isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> layer.get(2, 0));

        Char2D rotated = layer.rotate();
        Char2D mirrored = layer.mirror();
        assertEquals(3, rotated.xSize());
        assertEquals(2, rotated.zSize());
        assertEquals('e', rotated.get(0, 0));
        assertEquals('a', mirrored.get(1, 0));
        assertEquals('f', mirrored.get(0, 2));
    }

    @Test
    void char3dAggregatesLayersAndFindsChars() {
        Char2D l1 = new Char2D(List.of("ab", "cd"));
        Char2D l2 = new Char2D(List.of("ef", "gh"));
        Char3D structure = new Char3D(List.of(l1, l2));

        assertEquals(2, structure.xSize());
        assertEquals(2, structure.ySize());
        assertEquals(2, structure.zSize());
        assertEquals(new Vec3i(2, 2, 2), structure.size());
        assertEquals('g', structure.get(0, 1, 1));
        assertEquals(1, structure.count('g'));
        assertTrue(structure.contains('a'));
        assertFalse(structure.contains('x'));
        assertEquals(new Vec3i(0, 1, 1), structure.find('g').orElseThrow());
        assertTrue(structure.find('x').isEmpty());
    }

    @Test
    void char3dRejectsVaryingLayerSizes() {
        Char2D a = new Char2D(List.of("ab", "cd"));
        Char2D b = new Char2D(List.of("abc", "def"));
        assertThrows(IllegalArgumentException.class, () -> new Char3D(List.of(a, b)));
    }

    @Test
    void markedChar3dTracksMarkAndWorldOrigin() {
        Char3D base = new Char3D(List.of(
            new Char2D(List.of("ab", "cd")),
            new Char2D(List.of("ef", "gh"))
        ));

        assertThrows(IllegalArgumentException.class, () -> new MarkedChar3D(base, new Vec3i(3, 0, 0)));
        MarkedChar3D marked = new MarkedChar3D(base, new Vec3i(1, 0, 1));
        assertEquals(new Vec3i(1, 0, 1), marked.mark());
        assertEquals(new BlockPos(9, 64, 9), marked.getOriginInWorld(new BlockPos(10, 64, 10)));

        MarkedChar3D rotated = marked.rotate();
        MarkedChar3D mirrored = marked.mirror();
        assertEquals(new Vec3i(1, 0, 0), rotated.mark());
        assertEquals(new Vec3i(0, 0, 1), mirrored.mark());
    }

    @Test
    void markedTransformableChar3dProducesTransformSet() {
        Char3D base = new Char3D(List.of(
            new Char2D(List.of("ab", "cd")),
            new Char2D(List.of("ef", "gh"))
        ));
        MarkedTransformableChar3D transformed = new MarkedTransformableChar3D(base, new Vec3i(0, 0, 0));
        List<MarkedChar3D> all = new ArrayList<>();
        transformed.iterator().forEachRemaining(all::add);

        assertTrue(all.size() >= 4);
        assertEquals(all.getFirst(), transformed.getOriginal());
        assertEquals(transformed, new MarkedTransformableChar3D(base, new Vec3i(0, 0, 0)));
    }
}
