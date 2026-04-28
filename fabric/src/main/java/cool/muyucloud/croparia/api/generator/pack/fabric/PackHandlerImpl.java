package cool.muyucloud.croparia.api.generator.pack.fabric;

import cool.muyucloud.croparia.CropariaIf;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackHandlerImpl {
    public static void forEachJar(BiConsumer<File, String> consumer) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            String modId = mod.getMetadata().getId();
            try {
                mod.getOrigin().getPaths().forEach(path -> consumer.accept(path.toFile(), modId));
            } catch (UnsupportedOperationException e) {
                CropariaIf.LOGGER.debug("Cannot scan generators from mod %s".formatted(modId));
            }
        });
    }
}
