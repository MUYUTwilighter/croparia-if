package cool.muyucloud.croparia;

import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.api.core.command.CommonCommandRoot;
import cool.muyucloud.croparia.config.Config;
import cool.muyucloud.croparia.config.ConfigFileHandler;
import cool.muyucloud.croparia.registry.*;
import cool.muyucloud.croparia.util.Ref;
import cool.muyucloud.croparia.util.SidedRef;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Consumer;

public class CropariaIf {
    public static final String MOD_ID = "croparia";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Config CONFIG = ConfigFileHandler.load();
    private static Boolean SERVER_STARTED = false;
    private static MinecraftServer SERVER = null;

    public static void init() {
        OnLoadSupplier.LAST_DATA_LOAD = System.currentTimeMillis();
        CropariaIf.LOGGER.info("=== Croparia common setup ===");
        LOGGER.info("Croparia IF customize registration");
        DgRegistries.register();
        DataGenerators.register();
        PackHandlers.register();
        Crops.register();
        Elements.register();
        LOGGER.info("Croparia IF vanilla registration");
        NetworkHandlers.register();
        CropariaComponents.register();
        CropariaBlocks.register();
        BlockEntities.register();
        CropariaItems.register();
        SlotDisplays.register();
        Recipes.register();
        Tabs.register();
        CommonCommandRoot.register();
        PlacedFeatures.register();
        LOGGER.info("Croparia IF event registration");
        LifecycleEvent.SERVER_BEFORE_START.register(server -> SERVER = server);
        LifecycleEvent.SERVER_STARTING.register(server -> ConfigFileHandler.reload(CONFIG));
        LifecycleEvent.SERVER_STARTED.register(server -> {
            SERVER_STARTED = true;
            OnLoadSupplier.LAST_DATA_LOAD = System.currentTimeMillis();
            if (CONFIG.getAutoReload() >= 0) {
                LOGGER.info("Croparia IF is performing a datapack reload to apply data generators");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "schedule function croparia:auto_reload %s".formatted(CONFIG.getAutoReload()));
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            SERVER_STARTED = false;
            ConfigFileHandler.save(CONFIG);
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> SERVER = null);
        CropariaIf.LOGGER.info("=== Croparia common setup done ===");
    }

    public static Optional<MinecraftServer> getServer() {
        return Optional.ofNullable(SERVER);
    }

    public static void ifServer(Consumer<MinecraftServer> consumer) {
        if (SERVER != null) consumer.accept(SERVER);
    }

    public static ResourceLocation of(String path) {
        ResourceLocation id = ResourceLocation.tryBuild(MOD_ID, path);
        if (id == null) {
            throw new IllegalArgumentException("Invalid path: " + path);
        } else {
            return id;
        }
    }

    public static boolean isServerStarted() {
        return SERVER_STARTED;
    }

    public static Optional<RegistryAccess> getRegistryAccess() {
        Ref<RegistryAccess> accessRef = new Ref<>();
        SidedRef.ifServerOrElse(
            () -> CropariaIf.ifServer(server -> accessRef.set(server.registryAccess())),
            () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    accessRef.set(level.registryAccess());
                }
            }
        );
        return Optional.ofNullable(accessRef.get());
    }
}
