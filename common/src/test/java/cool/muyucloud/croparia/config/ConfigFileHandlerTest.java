package cool.muyucloud.croparia.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileHandlerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void resetSupplier() {
        ConfigFileHandler.resetGameFolderSupplierForTest();
    }

    @Test
    void loadCreatesDefaultConfigWhenFileMissing() {
        ConfigFileHandler.setGameFolderSupplierForTest(() -> tempDir);

        Config loaded = ConfigFileHandler.load();
        Path configPath = tempDir.resolve("config/croparia.json");

        assertTrue(Files.exists(configPath));
        assertEquals(tempDir.resolve("croparia"), loaded.getFilePath());
        assertEquals(tempDir.resolve("croparia/recipe_wizard/dump"), loaded.getRecipeWizard());
        assertEquals(20, loaded.getAutoReload());
        assertEquals(1, loaded.getSoakAttempts());
        assertTrue(loaded.getOverride());
    }

    @Test
    void saveThenLoadRoundTripKeepsConfiguredValues() {
        ConfigFileHandler.setGameFolderSupplierForTest(() -> tempDir);
        Config source = new Config(new RawConfig(
            tempDir.resolve("custom/data").toString(),
            tempDir.resolve("custom/wizard").toString(),
            false, true, false, true, 33, 6,
            List.of("minecraft:wheat", "@fabric.*")
        ));

        ConfigFileHandler.save(source);
        Config loaded = ConfigFileHandler.load();

        assertEquals(tempDir.resolve("custom/data"), loaded.getFilePath());
        assertEquals(tempDir.resolve("custom/wizard"), loaded.getRecipeWizard());
        assertFalse(loaded.getOverride());
        assertTrue(loaded.getInfusor());
        assertFalse(loaded.getRitual());
        assertTrue(loaded.getFruitUse());
        assertEquals(33, loaded.getAutoReload());
        assertEquals(6, loaded.getSoakAttempts());
        assertEquals(List.of("minecraft:wheat", "@fabric.*"), loaded.getBlacklist());
    }

    @Test
    void reloadCopiesValuesFromPersistedConfig() {
        ConfigFileHandler.setGameFolderSupplierForTest(() -> tempDir);
        Config target = new Config(new RawConfig(
            tempDir.resolve("old/data").toString(),
            tempDir.resolve("old/wizard").toString(),
            true, true, true, true, 20, 1, List.of()
        ));
        Config source = new Config(new RawConfig(
            tempDir.resolve("new/data").toString(),
            tempDir.resolve("new/wizard").toString(),
            false, false, false, false, 9, 2, List.of("minecraft:carrot", "@neo.*")
        ));
        ConfigFileHandler.save(source);

        ConfigFileHandler.reload(target);

        assertEquals(tempDir.resolve("new/data"), target.getFilePath());
        assertEquals(tempDir.resolve("new/wizard"), target.getRecipeWizard());
        assertFalse(target.getOverride());
        assertFalse(target.getInfusor());
        assertFalse(target.getRitual());
        assertFalse(target.getFruitUse());
        assertEquals(9, target.getAutoReload());
        assertEquals(2, target.getSoakAttempts());
        assertEquals(List.of("minecraft:carrot", "@neo.*"), target.getBlacklist());
    }
}
