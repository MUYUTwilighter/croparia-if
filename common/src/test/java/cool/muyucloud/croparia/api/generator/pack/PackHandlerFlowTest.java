package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.JsonObject;
import cool.muyucloud.croparia.api.generator.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static cool.muyucloud.croparia.TestSupport.rl;
import static org.junit.jupiter.api.Assertions.*;

class PackHandlerFlowTest {
    @Test
    void onTriggeredRunsLifecycleInOrderAndClearsCache(@TempDir Path tempDir) {
        ProbePackHandler handler = new ProbePackHandler(tempDir, false);

        handler.onTriggered();

        assertEquals(List.of("readBuiltin", "readGenerators", "generate", "onGenerated", "dump", "onDumped"), handler.getStages());
        assertTrue(handler.isCacheVisibleInOnGenerated());
        assertTrue(handler.occupy(handler.owner(), "cache/a.txt").isEmpty());
        assertTrue(Files.exists(tempDir.resolve("pack.mcmeta")));
    }

    @Test
    void onTriggeredClearsDumpRootWhenOverrideEnabled(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("stale.txt"), "stale", StandardCharsets.UTF_8);
        ProbePackHandler handler = new ProbePackHandler(tempDir, true);

        handler.onTriggered();

        assertEquals(1, handler.getClearCalls());
        assertFalse(Files.exists(tempDir.resolve("stale.txt")));
        assertTrue(Files.exists(tempDir.resolve("pack.mcmeta")));
    }

    private static final class ProbePackHandler extends PackHandler {
        private final List<String> stages = new ArrayList<>();
        private final DataGenerator owner = null;
        private int clearCalls;
        private boolean cacheVisibleInOnGenerated;

        private ProbePackHandler(Path root, boolean override) {
            super(
                rl("croparia_test", "pack_" + UUID.randomUUID()),
                root,
                new JsonObject(),
                () -> override
            );
        }

        @Override
        public void clear() {
            clearCalls++;
            super.clear();
        }

        @Override
        protected void readBuiltinGenerators() {
            stages.add("readBuiltin");
        }

        @Override
        protected void readGenerators() {
            stages.add("readGenerators");
        }

        @Override
        protected void generate() {
            stages.add("generate");
            this.cache("cache/a.txt", "value", owner);
        }

        @Override
        protected void onGenerated() {
            stages.add("onGenerated");
            Optional<String> value = this.occupy(owner, "cache/a.txt");
            cacheVisibleInOnGenerated = value.isPresent();
        }

        @Override
        protected void dump() {
            stages.add("dump");
        }

        @Override
        protected void onDumped() {
            stages.add("onDumped");
        }

        private List<String> getStages() {
            return stages;
        }

        private int getClearCalls() {
            return clearCalls;
        }

        private boolean isCacheVisibleInOnGenerated() {
            return cacheVisibleInOnGenerated;
        }

        private DataGenerator owner() {
            return owner;
        }
    }
}
