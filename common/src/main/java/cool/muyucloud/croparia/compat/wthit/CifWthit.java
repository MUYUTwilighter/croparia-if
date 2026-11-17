package cool.muyucloud.croparia.compat.wthit;

import cool.muyucloud.croparia.api.crop.block.*;
import mcp.mobius.waila.api.*;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class CifWthit implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        CropEntityBlockProvider provider = new CropEntityBlockProvider();
        registrar.head(provider, CropariaCrop.class);
        registrar.head(provider, MelonBlock.class);
        registrar.head(provider, MelonAttach.class);
        registrar.head(provider, MelonStem.class);
    }

    public static class CropEntityBlockProvider implements IBlockComponentProvider {
        @Override
        public void appendHead(@NonNull ITooltip tooltip, @NonNull IBlockAccessor accessor, @NonNull IPluginConfig config) {
            if (accessor.getBlock() instanceof CropEntityBlock<?> crop) {
                tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, crop.getName(accessor.getLevel(), accessor.getPosition()));
            }
        }
    }
}
