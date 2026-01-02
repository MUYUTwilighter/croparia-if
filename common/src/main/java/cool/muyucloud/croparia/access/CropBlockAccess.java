package cool.muyucloud.croparia.access;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Unique;

public interface CropBlockAccess {
    static CropBlockAccess of(CropBlock block) {
        return (CropBlockAccess) block;
    }

    @Unique
    IntegerProperty cif$getAgeProperty();
}
