package cool.muyucloud.croparia.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FabricLoaderSmokeTest {
    @Test
    void fabricLoaderBootstraps() {
        assertNotNull(FabricLoader.getInstance());
    }
}
