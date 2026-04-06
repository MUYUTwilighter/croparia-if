package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.JsonObject;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.mixin.ReloadableResourceManagerImplMixin;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.Ref;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
        PackHandler.register(pack);
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

    public ResourcePackHandler(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        super(id, path, meta, override);
    }

    private final PathPackResources resourcePack = new PathPackResources(
        this.getId().toString(), this.getRoot(), true
    );

    @Override
    public void onClientStopping(Ref<Minecraft> client) {
        super.onClientStopping(client);
        if (this.canOverride()) this.clear();
    }

    public PackResources getResourcePack() {
        return this.resourcePack;
    }

    @Override
    public void clear() {
        File file = this.getRoot().resolve("assets").toFile();
        if (file.isDirectory()) {
            CropariaIf.LOGGER.info("Clearing resource pack directory");
            try {
                FileUtil.deleteDir(file);
            } catch (Throwable e) {
                CropariaIf.LOGGER.error("Failed to clear resource pack directory", e);
            }
        }
    }

    @Override
    public Path getDumpRoot() {
        return this.getRoot().resolve("assets");
    }
}
