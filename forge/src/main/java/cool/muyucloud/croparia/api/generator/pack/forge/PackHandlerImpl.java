package cool.muyucloud.croparia.api.generator.pack.forge;

import cool.muyucloud.croparia.CropariaIf;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackHandlerImpl {
    public static void forEachJar(BiConsumer<File, String> consumer) {
        ModList.get().getMods().forEach(mod -> {
            String modId = mod.getModId();
            try {
                consumer.accept(mod.getOwningFile().getFile().getFilePath().toFile(), modId);
            } catch (UnsupportedOperationException e) {
                CropariaIf.LOGGER.debug("Cannot scan generators from mod %s".formatted(modId));
            }
        });
    }
}
