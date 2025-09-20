package cool.muyucloud.croparia.api.generator.pack.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackHandlerImpl {
    public static void forEachJar(BiConsumer<File, String> consumer) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            try {
                mod.getOrigin().getPaths().forEach(path -> {
                    consumer.accept(path.toFile(), mod.getMetadata().getId());
                });
            } catch (UnsupportedOperationException ignored) {
                // JIJ does not support getOrigin().getPaths()
            }
        });
    }
}
