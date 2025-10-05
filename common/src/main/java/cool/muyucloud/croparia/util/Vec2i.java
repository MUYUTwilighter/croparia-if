package cool.muyucloud.croparia.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record Vec2i(int x, int z)  implements Comparable<Vec2i> {
    public static final Codec<Vec2i> CODEC = Codec.INT.listOf(2, 2).xmap(ints -> new Vec2i(ints.getFirst(), ints.get(1)), Vec2i::toList);

    public Vec3i toVec3i(int y) {
        return new Vec3i(x, y, z);
    }

    public static Vec2i of(int x, int z) {
        return new Vec2i(x, z);
    }

    public static Vec2i of(Vec3i vec) {
        return of(vec.getX(), vec.getZ());
    }

    public List<Integer> toList() {
        return List.of(x, z);
    }

    @Override
    public int compareTo(@NotNull Vec2i o) {
        return this.x != o.x ? Integer.compare(this.x, o.x) : Integer.compare(this.z, o.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec2i(int x1, int z1))) return false;
        return x == x1 && z == z1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
