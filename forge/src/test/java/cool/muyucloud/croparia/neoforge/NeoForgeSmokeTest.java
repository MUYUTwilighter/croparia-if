package cool.muyucloud.croparia.neoforge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NeoForgeSmokeTest {
    @Test
    void commonClassesAreAvailableOnTestRuntime() throws ClassNotFoundException {
        assertNotNull(Class.forName("cool.muyucloud.croparia.util.Vec2i"));
    }
}
