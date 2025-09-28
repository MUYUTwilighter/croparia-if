package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.JsonObject;
import cool.muyucloud.croparia.mixin.ReloadableResourceManagerImplMixin;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Path-based resource pack handler, representing a resource pack stored in a directory.
 * <p>
 * Use {@link #register(ResourcePackHandler)} to insert your pack into Minecraft's resource pack list.
 *
 * @see ReloadableResourceManagerImplMixin
 *
 */
@SuppressWarnings("unused")
public class ResourcePackHandler extends PackHandler {
    public static final Map<ResourceLocation, ResourcePackHandler> REGISTRY = new HashMap<>();

    public static <P extends ResourcePackHandler> P register(P pack) {
        REGISTRY.put(pack.getId(), pack);
        return pack;
    }

    /**
     * Register a new resource pack handler so that it will be loaded by Minecraft.
     *
     * @see ReloadableResourceManagerImplMixin
     *
     */
    public static ResourcePackHandler register(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        return register(new ResourcePackHandler(id, path, meta, override));
    }

    private final PathPackResources resourcePack = new PathPackResources(
        new PackLocationInfo(
            this.getId().toString(),
            Texts.literal(this.getId().toString()),
            PackSource.BUILT_IN,
            Optional.empty()
        ), getRoot()
    );

    public ResourcePackHandler(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        super(id, path, meta, override);
    }

    public PackResources getResourcePack() {
        return this.resourcePack;
    }

    @Override
    public String proxyPath(String path) {
        return path.startsWith("assets/") ? path : "assets/" + path;
    }
}
