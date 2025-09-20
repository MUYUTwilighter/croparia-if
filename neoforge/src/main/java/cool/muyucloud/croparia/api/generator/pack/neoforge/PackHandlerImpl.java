package cool.muyucloud.croparia.api.generator.pack.neoforge;

import net.neoforged.fml.ModList;

import java.io.File;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackHandlerImpl {
    public static void forEachJar(BiConsumer<File, String> consumer) {
        ModList.get().getMods().forEach(mod -> consumer.accept(mod.getOwningFile().getFile().getFilePath().toFile(), mod.getModId()));
    }
}
