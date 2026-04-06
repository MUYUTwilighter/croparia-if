package cool.muyucloud.croparia.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record Vec2i(int x, int z)  implements Comparable<Vec2i> {
    public static final Codec<Vec2i> CODEC = Codec.INT.listOf().comapFlatMap(
        ints -> ints.size() == 2 ? com.mojang.serialization.DataResult.success(new Vec2i(ints.get(0), ints.get(1))) : com.mojang.serialization.DataResult.error(() -> "Expected 2 integers"),
        Vec2i::toList
    );

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
        if (!(o instanceof Vec2i other)) return false;
        return x == other.x() && z == other.z();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
