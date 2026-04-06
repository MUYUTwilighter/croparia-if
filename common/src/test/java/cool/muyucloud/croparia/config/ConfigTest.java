package cool.muyucloud.croparia.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    void rawConfigDeserializationAndBlacklistFilteringWork() {
        String base = Path.of(System.getProperty("java.io.tmpdir"), "croparia_config_test").toAbsolutePath().toString();
        RawConfig raw = new RawConfig(
            Path.of(base, "data").toString(),
            Path.of(base, "wizard").toString(),
            false,
            true,
            false,
            true,
            42,
            3,
            2,
            2,
            List.of("minecraft:wheat", "@fabric.*", "not a resource location")
        );

        Config config = new Config(raw);

        assertEquals(Path.of(base, "data"), config.getFilePath());
        assertEquals(Path.of(base, "wizard"), config.getRecipeWizard());
        assertEquals(42, config.getAutoReload());
        assertEquals(3, config.getSoakAttempts());
        assertFalse(config.getOverride());
        assertTrue(config.getInfusor());
        assertFalse(config.getRitual());
        assertTrue(config.getFruitUse());
        assertEquals(1, config.getCropBlackList().size());
        assertEquals(1, config.getModBlackList().size());
        assertFalse(config.isCropValid(ResourceLocation.fromNamespaceAndPath("minecraft", "wheat")));
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
            20,
            1,
            2,
            2,
            List.of()
        ));

        config.setBlackList(List.of("minecraft:carrot", "@neo.*"));
        assertEquals(List.of("minecraft:carrot", "@neo.*"), config.getBlacklist());
        assertFalse(config.isCropValid(ResourceLocation.fromNamespaceAndPath("minecraft", "carrot")));
        assertFalse(config.isModValid("neoforge"));
    }
}
