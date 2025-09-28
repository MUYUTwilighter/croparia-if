package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.JsonObject;
import cool.muyucloud.croparia.api.generator.util.AlwaysEnabledFileResourcePackProvider;
import cool.muyucloud.croparia.mixin.PackRepositoryMixin;
import cool.muyucloud.croparia.mixin.ReloadableResourceManagerImplMixin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Path-based data pack handler, representing a data pack stored in a directory.
 * <p>
 * Use {@link #register(DataPackHandler)} to insert your pack into Minecraft's resource pack list.
 *
 * @see PackRepositoryMixin
 *
 */
public class DataPackHandler extends PackHandler {
    public static final Map<ResourceLocation, DataPackHandler> REGISTRY = new HashMap<>();

    public static <P extends DataPackHandler> P register(P pack) {
        REGISTRY.put(pack.getId(), pack);
        return pack;
    }

    /**
     * Register a new resource pack handler so that it will be loaded by Minecraft.
     *
     * @see ReloadableResourceManagerImplMixin
     *
     */
    public static DataPackHandler register(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        return register(new DataPackHandler(id, path, meta, override));
    }

    private final AlwaysEnabledFileResourcePackProvider datapack = new AlwaysEnabledFileResourcePackProvider(
        this.getId().toString(), getRoot(), PackType.SERVER_DATA, PackSource.BUILT_IN
    );

    public DataPackHandler(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        super(id, path, meta, override);
    }

    public AlwaysEnabledFileResourcePackProvider getDatapack() {
        return datapack;
    }

    @Override
    public String proxyPath(String path) {
        return path.startsWith("data/") ? path : "data/" + path;
    }
}