package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SyncCommandTest {
    @Test
    void bothRootsAcceptTheSameTargetsAndReloadFlags() {
        for (boolean client : new boolean[]{false, true}) {
            String root = client ? "croparia" : "cropariaServer";
            CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
            dispatcher.register(LiteralArgumentBuilder.<Object>literal(root).then(new SyncCommand<Object>(client).build()));
            for (String target : new String[]{"all", "croparia:crops", "croparia:melons"}) {
                for (String suffix : new String[]{"", " noReload"}) {
                    String command = root + " sync " + target + suffix;
                    var parsed = dispatcher.parse(command, new Object());
                    assertFalse(parsed.getReader().canRead(), command);
                    assertTrue(parsed.getExceptions().isEmpty(), command);
                    assertNotNull(parsed.getContext().getCommand(), command);
                }
            }
            assertNotNull(dispatcher.parse(root + " sync", new Object()).getContext().getCommand());
            assertTrue(dispatcher.parse(root + " sync all unknown", new Object()).getReader().canRead());
        }
    }

    @Test
    void reloadRetainsExternalDefinitionsAndInvalidatesNbt(@TempDir Path tempDir) {
        AtomicInteger externalReads = new AtomicInteger();
        AtomicInteger fileReads = new AtomicInteger();
        CropRegistry<AbstractCrop<?>> crops = new CropRegistry<>(tempDir, MapCodec.unitCodec(() -> null)) {
            @Override
            public void readCrops() {
                fileReads.incrementAndGet();
            }
        };
        crops.withExternal(() -> {
            externalReads.incrementAndGet();
            return Collections.emptyList();
        });
        crops.loadCrops();
        var encoded = crops.getNbt();
        assertSame(encoded, crops.getNbt());
        crops.loadCrops();
        assertEquals(2, externalReads.get());
        assertEquals(2, fileReads.get());
        assertNotSame(encoded, crops.getNbt());
    }

    @Test
    void suggestionsOnlyIncludeCropRegistries(@TempDir Path tempDir) {
        Identifier cropId = Identifier.parse("sync_test:crops");
        Identifier otherId = Identifier.parse("sync_test:other");
        CropRegistry<?> crops = new CropRegistry<>(tempDir, MapCodec.unitCodec(() -> null));
        DgRegistry<?> other = DgRegistry.ofMap(Map.of());
        DgRegistry.register(cropId, crops);
        DgRegistry.register(otherId, other);
        try {
            CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
            dispatcher.register(LiteralArgumentBuilder.<Object>literal("croparia").then(new SyncCommand<Object>(true).build()));
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("croparia sync ", new Object()))
                .join().getList().stream().map(suggestion -> suggestion.getText()).toList();
            assertTrue(suggestions.contains("all"));
            assertTrue(suggestions.contains(cropId.toString()));
            assertFalse(suggestions.contains(otherId.toString()));
        } finally {
            DgRegistry.REGISTRY.remove(cropId);
            DgRegistry.REGISTRY.remove(otherId);
            DgRegistry.BY_INSTANCE.remove(crops);
            DgRegistry.BY_INSTANCE.remove(other);
        }
    }
}
