package cool.muyucloud.croparia.util;

import net.jcip.annotations.Immutable;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Immutable
@SuppressWarnings("unused")
public class RangedVec3i extends Vec3i {
    public static RangedVec3i maxBounds(int maxX, int maxY, int maxZ, int x, int y, int z) {
        return new RangedVec3i(0, 0, 0, maxX, maxY, maxZ, x, y, z);
    }

    public static RangedVec3i maxBounds(int maxX, int maxY, int maxZ) {
        return maxBounds(maxX, maxY, maxZ, 0, 0, 0);
    }

    public static RangedVec3i bounds(int x, int y, int z, int length, int width, int height) {
        return new RangedVec3i(x, y, z, x + length, y + width, z + height, x, y, z);
    }

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int hash;

    /**
     * Auto clamps the values to the bounds
     *
     */
    public RangedVec3i(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int x, int y, int z) {
        super(Math.min(Math.max(minX, x), maxX),
            Math.min(Math.max(minY, y), maxY),
            Math.min(Math.max(minZ, z), maxZ));
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Min values must be less than or equal to max values");
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.hash = Objects.hash(minX, minY, minZ, maxX, maxY, maxZ, getX(), getY(), getZ());
    }

    public boolean isInside(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean isInside(Vec3i vec) {
        return isInside(vec.getX(), vec.getY(), vec.getZ());
    }

    public int volume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    @Override
    public @NotNull RangedVec3i multiply(int scalar) {
        return scalar == 0 ? this : new RangedVec3i(minX, minY, minZ, maxX, maxY, maxZ, getX() * scalar, getY() * scalar, getZ() * scalar);
    }

    @Override
    public @NotNull RangedVec3i relative(@NotNull Direction direction, int distance) {
        return distance == 0 ? this : this.offset(direction.getStepX() * distance, direction.getStepY() * distance, direction.getStepZ() * distance);
    }

    @Override
    public @NotNull RangedVec3i relative(@NotNull Direction.Axis axis, int amount) {
        return amount == 0 ? this : amount > 0 ? this.relative(axis.getPositive(), amount) : this.relative(axis.getNegative(), amount);
    }

    @Override
    public @NotNull RangedVec3i cross(@NotNull Vec3i vector) {
        return new RangedVec3i(minX, minY, minZ, maxX, maxY, maxZ, this.getY() * vector.getZ() - this.getZ() * vector.getY(), this.getZ() * vector.getX() - this.getX() * vector.getZ(), this.getX() * vector.getY() - this.getY() * vector.getX());
    }

    public RangedVec3i min(int minX, int minY, int minZ) {
        return new RangedVec3i(minX, minY, minZ, maxX, maxY, maxZ, getX(), getY(), getZ());
    }

    public RangedVec3i min(@NotNull Vec3i min) {
        return min(min.getX(), min.getY(), min.getZ());
    }

    public RangedVec3i minX(int minX) {
        return min(minX, minY, minZ);
    }

    public RangedVec3i minY(int minY) {
        return min(minX, minY, minZ);
    }

    public RangedVec3i minZ(int minZ) {
        return min(minX, minY, minZ);
    }

    public RangedVec3i max(int maxX, int maxY, int maxZ) {
        return new RangedVec3i(minX, minY, minZ, maxX, maxY, maxZ, getX(), getY(), getZ());
    }

    public RangedVec3i max(@NotNull Vec3i max) {
        return max(max.getX(), max.getY(), max.getZ());
    }

    public RangedVec3i maxX(int maxX) {
        return max(maxX, maxY, maxZ);
    }

    public RangedVec3i maxY(int maxY) {
        return max(maxX, maxY, maxZ);
    }

    public RangedVec3i maxZ(int maxZ) {
        return max(maxX, maxY, maxZ);
    }

    @Override
    public @NotNull RangedVec3i offset(int dx, int dy, int dz) {
        return this.rebuild(getX() + dx, getY() + dy, getZ() + dz);
    }

    public RangedVec3i rebuild(int x, int y, int z) {
        RangedVec3i result = new RangedVec3i(minX, minY, minZ, maxX, maxY, maxZ, x, y, z);
        if (result.equals(this)) {
            return this;
        } else {
            return result;
        }
    }

    public RangedVec3i rebuild(@NotNull Vec3i vec) {
        return rebuild(vec.getX(), vec.getY(), vec.getZ());
    }

    public RangedVec3i x(int x) {
        return rebuild(x, getY(), getZ());
    }

    public RangedVec3i y(int y) {
        return rebuild(getX(), y, getZ());
    }

    public RangedVec3i z(int z) {
        return rebuild(getX(), getY(), z);
    }

    public RangedVec3i dx(int dx) {
        return x(getX() + dx);
    }

    public RangedVec3i dy(int dy) {
        return y(getY() + dy);
    }

    public RangedVec3i dz(int dz) {
        return z(getZ() + dz);
    }

    public boolean testX(int x) {
        return x >= minX && x <= maxX;
    }

    public boolean testY(int y) {
        return y >= minY && y <= maxY;
    }

    public boolean testZ(int z) {
        return z >= minZ && z <= maxZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    @Override
    public @NotNull RangedVec3i above() {
        return above(1);
    }

    @Override
    public @NotNull RangedVec3i above(int distance) {
        return relative(Direction.UP, distance);
    }

    @Override
    public @NotNull RangedVec3i below() {
        return below(1);
    }

    @Override
    public @NotNull RangedVec3i below(int distance) {
        return relative(Direction.DOWN, distance);
    }

    @Override
    public @NotNull RangedVec3i north() {
        return north(1);
    }

    @Override
    public @NotNull RangedVec3i north(int distance) {
        return relative(Direction.NORTH, distance);
    }

    @Override
    public @NotNull RangedVec3i south() {
        return south(1);
    }

    @Override
    public @NotNull RangedVec3i south(int distance) {
        return relative(Direction.SOUTH, distance);
    }

    @Override
    public @NotNull RangedVec3i west() {
        return west(1);
    }

    @Override
    public @NotNull RangedVec3i west(int distance) {
        return relative(Direction.WEST, distance);
    }

    @Override
    public @NotNull RangedVec3i east() {
        return east(1);
    }

    @Override
    public @NotNull RangedVec3i east(int distance) {
        return relative(Direction.EAST, distance);
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            if (o instanceof RangedVec3i that) {
                return minX == that.minX && minY == that.minY && minZ == that.minZ && maxX == that.maxX && maxY == that.maxY && maxZ == that.maxZ;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
