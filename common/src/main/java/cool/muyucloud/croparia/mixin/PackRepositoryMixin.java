package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.pack.DataPackHandler;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin class used to insert our datapack provided by {@link DataPackHandler} and trigger the data-generation,
 * so that the datapack can be loaded.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    /**
     * Trigger data generation registered for each {@link DataPackHandler} before the datapack reload starts.
     */
    @Inject(method = "reload", at = @At("HEAD"))
    private void onReload(CallbackInfo ci) {
        CropariaIf.getServer().ifPresentOrElse(server -> {
            if (server.isSameThread()) {
                cif$trigger();
            }
        }, this::cif$trigger);
    }

    @Unique
    private void cif$trigger() {
        DataGenerator.LOGGER.debug("=== Data Pack Reload Triggered ===");
        DataPackHandler.REGISTRY.values().forEach(DataPackHandler::onTriggered);
    }

    /**
     * Insert {@link DataPackHandler} into Minecraft
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static RepositorySource[] insertProviders(RepositorySource... providers) {
        DataGenerator.LOGGER.debug("=== Inserting Data Pack Providers ===");
        RepositorySource[] newProviders = new RepositorySource[providers.length + DataPackHandler.REGISTRY.size()];
        int i = 0;
        for (DataPackHandler pack : DataPackHandler.REGISTRY.values()) {
            newProviders[i] = pack.getDatapack();
            i++;
        }
        for (RepositorySource source : providers) {
            newProviders[i] = source;
            i++;
        }
        return newProviders;
    }
}
