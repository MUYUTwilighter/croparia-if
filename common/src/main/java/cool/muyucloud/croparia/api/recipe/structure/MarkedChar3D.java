package cool.muyucloud.croparia.api.recipe.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.List;
import java.util.Objects;

/**
 * A structure with a mark position in it, this mark position is used for locating the center of the structure.
 * @implNote field {@link #mark} is not considered to be part of the structure, but determines the origin of the structure in the world.
 * */
@SuppressWarnings("unused")
public class MarkedChar3D extends Char3D {
    private final Vec3i mark;

    public MarkedChar3D(Char3D pattern, Vec3i mark) {
        super(pattern);
        if (mark.getX() >= this.xSize() || mark.getY() >= this.ySize() || mark.getZ() >= this.zSize() || mark.getX() < 0 || mark.getY() < 0 || mark.getZ() < 0) {
            throw new IllegalArgumentException("Mark position out of bounds");
        }
        this.mark = mark;
    }

    public MarkedChar3D(List<Char2D> structure, Vec3i mark) {
        super(structure);
        if (mark.getX() >= this.xSize() || mark.getY() >= this.ySize() || mark.getZ() >= this.zSize() || mark.getX() < 0 || mark.getY() < 0 || mark.getZ() < 0) {
            throw new IllegalArgumentException("Mark position out of bounds");
        }
        this.mark = mark;
    }

    public Vec3i mark() {
        return mark;
    }

    @Override
    public MarkedChar3D rotate() {
        int newX = mark.getZ();
        int newZ = zSize() - 1 - mark.getX();
        Vec3i rotatedMark = new Vec3i(newX, mark.getY(), newZ);
        return new MarkedChar3D(super.rotate(), rotatedMark);
    }

    @Override
    public MarkedChar3D mirror() {
        int newX = xSize() - 1 - mark.getX();
        int newZ = mark.getZ();
        Vec3i mirroredMark = new Vec3i(newX, mark.getY(), newZ);
        return new MarkedChar3D(super.mirror(), mirroredMark);
    }

    public BlockPos getOriginInWorld(BlockPos markInWorld) {
        return markInWorld.subtract(mark);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MarkedChar3D char3d)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(mark, char3d.mark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mark);
    }
}
