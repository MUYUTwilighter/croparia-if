package cool.muyucloud.croparia.api.generator.pack.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import net.neoforged.fml.ModList;

import java.io.File;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackHandlerImpl {
    public static void forEachJar(BiConsumer<File, String> consumer) {
        ModList.get().getMods().forEach(mod -> {
            String modId = mod.getModId();
            try {
                File file = mod.getOwningFile().getFile().getFilePath().toFile();
                consumer.accept(file, modId);
            } catch (UnsupportedOperationException e) {
                CropariaIf.LOGGER.error("Failed to scan generators from mod %s".formatted(modId), e);
            }
        });
    }
}
