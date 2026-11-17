package cool.muyucloud.croparia.api.crop.util;

import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropRegistry;

public interface CropRegistryAccess<C extends AbstractCrop<?>> {
    CropRegistry<? super C> getRegistry();
}
