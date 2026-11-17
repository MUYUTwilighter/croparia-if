package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.pack.DataPackHandler;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin class used to insert our datapack provided by {@link DataPackHandler} so that the datapack can be loaded.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    /**
     * Insert {@link DataPackHandler} into Minecraft
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, name = "sources")
    private static RepositorySource[] insertProviders(RepositorySource... sources) {
        DataGenerator.LOGGER.debug("=== Inserting Data Pack Providers ===");
        RepositorySource[] newProviders = new RepositorySource[sources.length + DataPackHandler.REGISTRY.size()];
        int i = 0;
        for (DataPackHandler pack : DataPackHandler.REGISTRY.values()) {
            pack.onTriggered();
            newProviders[i] = pack.getDatapack();
            i++;
        }
        for (RepositorySource source : sources) {
            newProviders[i] = source;
            i++;
        }
        return newProviders;
    }
}
