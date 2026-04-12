package cool.muyucloud.croparia.config;

import cool.muyucloud.croparia.util.Dependencies;
import dev.architectury.platform.Platform;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAndDependenciesFabricTest {
    @Test
    void parseAndResolvePathRespectGameFolder() {
        Path gameFolder = Platform.getGameFolder();
        Path relative = Config.parsePath("croparia/config").orElseThrow();
        assertEquals(gameFolder.resolve("croparia/config"), relative);

        String resolvedRelative = Config.resolvePath(gameFolder.resolve("croparia/config"));
        assertEquals(Path.of("croparia/config").toString(), resolvedRelative);

        String absolute = Config.resolvePath(Path.of("C:/tmp/croparia/config.json"));
        assertTrue(Path.of(absolute).isAbsolute());
    }

    @Test
    void blacklistParsingAndValidationWork() {
        Config config = new Config();
        config.setBlackList(List.of("minecraft:wheat", "@fabric.*", "not a rl"));

        assertEquals(1, config.getCropBlackList().size());
        assertEquals(1, config.getModBlackList().size());
        assertFalse(config.isModValid("fabricloader"));
        assertTrue(config.isModValid("minecraft"));
        assertFalse(config.isCropValid(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "wheat")));
    }

    @Test
    void dependenciesAvailabilityUsesAnyWithinGroupAndAllAcrossGroups() {
        Dependencies satisfiable = new Dependencies(List.of(
            List.of("minecraft", "definitely_missing_mod_x"),
            List.of("fabricloader")
        ));
        assertTrue(satisfiable.available());

        Dependencies unsatisfied = new Dependencies(List.of(
            List.of("minecraft"),
            List.of("definitely_missing_mod_x")
        ));
        assertFalse(unsatisfied.available());

        Dependencies empty = new Dependencies(List.of(List.of(), List.of()));
        assertTrue(empty.isEmpty());
    }
}

