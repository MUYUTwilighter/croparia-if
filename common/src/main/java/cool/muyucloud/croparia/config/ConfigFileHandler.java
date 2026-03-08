package cool.muyucloud.croparia.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import cool.muyucloud.croparia.util.FileUtil;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class ConfigFileHandler {
    public static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Supplier<Path> gameFolderSupplier = Platform::getGameFolder;

    private static Path configPath() {
        return gameFolderSupplier.get().resolve("config/croparia.json");
    }

    private static Config defaultConfig() {
        Path gameFolder = gameFolderSupplier.get();
        return new Config(new RawConfig(
            gameFolder.resolve("croparia").toString(),
            gameFolder.resolve("croparia/recipe_wizard/dump").toString(),
            true, true, true, true, 20, 1, List.of()
        ));
    }

    static void setGameFolderSupplierForTest(Supplier<Path> supplier) {
        gameFolderSupplier = supplier;
    }

    static void resetGameFolderSupplierForTest() {
        gameFolderSupplier = Platform::getGameFolder;
    }

    public static void save(Config config) {
        LOGGER.info("Saving config");
        Path configPath = configPath();
        try {
            FileUtil.ensureParentDirectory(configPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create config directory", e);
        }
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(configPath, StandardCharsets.UTF_8))) {
            writer.setIndent("  ");
            GSON.toJson(config.toRaw(), RawConfig.class, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static Config load() {
        Config config;
        Path configPath = configPath();
        try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            config = new Config(GSON.fromJson(reader, RawConfig.class));
        } catch (Exception e) {
            LOGGER.warn("Config file not found or could not be read, creating a new one");
            config = defaultConfig();
        }
        save(config);
        return config;
    }

    public static void reload(Config config) {
        LOGGER.info("Loading config");
        Config newConfig = load();
        config.setAutoReload(newConfig.getAutoReload());
        config.setSoakAttempts(newConfig.getSoakAttempts());
        config.setFilePath(newConfig.getFilePath());
        config.setRecipeWizard(newConfig.getRecipeWizard());
        config.setOverride(newConfig.getOverride());
        config.setFruitUse(newConfig.getFruitUse());
        config.setInfusor(newConfig.getInfusor());
        config.setRitual(newConfig.getRitual());
        config.setBlackList(newConfig.getBlacklist());
    }
}
