package cool.muyucloud.croparia.api.crop;

import org.jetbrains.annotations.Nullable;

public interface CropAccess<C extends AbstractCrop<?>> {
    C getCrop();

    @SuppressWarnings("unchecked")
    static <C extends AbstractCrop<?>> @Nullable C tryGet(CropAccess<?> access) {
        AbstractCrop<?> crop = access.getCrop();
        try {
            return (C) crop;
        } catch (ClassCastException e) {
            return null;
        }
    }
}
