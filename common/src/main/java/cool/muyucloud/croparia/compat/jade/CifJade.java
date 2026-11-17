package cool.muyucloud.croparia.compat.jade;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.block.*;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

@WailaPlugin
public class CifJade implements IWailaPlugin {
    @Override
    public void registerClient(@NonNull IWailaClientRegistration registration) {
        CropEntityBlockProvider provider = new CropEntityBlockProvider();
        registration.registerBlockComponent(provider, CropariaCrop.class);
        registration.registerBlockComponent(provider, MelonBlock.class);
        registration.registerBlockComponent(provider, MelonStem.class);
        registration.registerBlockComponent(provider, MelonAttach.class);
    }

    public static class CropEntityBlockProvider implements IBlockComponentProvider {
        @Override
        public void appendTooltip(@NonNull ITooltip builder, @NonNull BlockAccessor accessor, @NonNull IPluginConfig iPluginConfig) {
            if (accessor.getBlock() instanceof CropEntityBlock<?> block) {
                builder.replace(
                    JadeIds.CORE_OBJECT_NAME,
                    IThemeHelper.get().title(block.getName(accessor.getLevel(), accessor.getPosition()))
                );
            }
        }

        @Override
        public @NonNull Identifier getUid() {
            return CropariaIf.of("crop");
        }
    }
}
