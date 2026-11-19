package cool.muyucloud.croparia.config;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    void rawConfigDeserializationAndBlacklistFilteringWork() {
        String base = Path.of(System.getProperty("java.io.tmpdir"), "croparia_config_test").toAbsolutePath().toString();
        Path dataPath = Path.of(base, "data");
        Path wizardPath = Path.of(base, "wizard");
        RawConfig raw = new RawConfig(
            dataPath.toString(),
            wizardPath.toString(),
            false,
            true,
            false,
            true,
            false,
            true,
            3,
            2,
            2,
            List.of("resource", "custom"),
            List.of("minecraft:wheat", "@fabric.*", "not a resource location")
        );

        Config config = new Config(raw);

        assertEquals(dataPath, config.getFilePath());
        assertEquals(wizardPath, config.getRecipeWizard());
        assertTrue(config.getAutoReload());
        assertEquals(3, config.getSoakAttempts());
        assertFalse(config.getOverride());
        assertTrue(config.getInfusor());
        assertFalse(config.getRitual());
        assertTrue(config.getFruitUse());
        assertFalse(config.getBreakEffect());
        assertEquals(List.of("resource", "custom"), config.getCropTypes());
        assertEquals(1, config.getCropBlackList().size());
        assertEquals(1, config.getModBlackList().size());
        assertFalse(config.isCropValid(Identifier.fromNamespaceAndPath("minecraft", "wheat")));
        assertFalse(config.isModValid("fabricloader"));
        assertTrue(config.isModValid("minecraft"));
    }

    @Test
    void getAndSetBlacklistRoundTrip() {
        String absolute = Path.of(System.getProperty("java.io.tmpdir"), "croparia_config_roundtrip").toAbsolutePath().toString();
        Config config = new Config(new RawConfig(
            absolute,
            absolute,
            true,
            true,
            true,
            true,
            true,
            true,
            1,
            2,
            2,
            List.of(),
            List.of()
        ));

        config.setBlackList(List.of("minecraft:carrot", "@neo.*"));
        assertEquals(List.of("minecraft:carrot", "@neo.*"), config.getBlacklist());
        assertFalse(config.isCropValid(Identifier.fromNamespaceAndPath("minecraft", "carrot")));
        assertFalse(config.isModValid("neoforge"));
    }
}
