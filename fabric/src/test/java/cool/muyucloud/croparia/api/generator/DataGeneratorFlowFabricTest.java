package cool.muyucloud.croparia.api.generator;

import com.google.gson.JsonObject;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import cool.muyucloud.croparia.api.placeholder.Template;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataGeneratorFlowFabricTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void dataGeneratorRespectsEnabledAndShouldLoad(@TempDir Path tempDir) {
        TestPackHandler pack = new TestPackHandler(tempDir);
        TestEntry enabled = new TestEntry("enabled", true);
        TestEntry skipped = new TestEntry("skipped", false);
        DataGenerator generator = new DataGenerator(
            true,
            true,
            List.of(),
            new Template("out/${id}.txt"),
            DgRegistry.ofMap(linkedMap(enabled, skipped)),
            new Template("v=${id}")
        );

        generator.generate(pack);

        assertEquals(1, pack.getAll(generator).size());
        assertEquals("v=croparia_test:enabled", pack.queryOwned(generator, "out/croparia_test:enabled.txt").orElseThrow());

        DataGenerator disabled = new DataGenerator(
            false,
            true,
            List.of(),
            new Template("out/${id}.txt"),
            DgRegistry.ofMap(linkedMap(enabled)),
            new Template("x")
        );
        disabled.generate(pack);
        assertTrue(pack.getAll(disabled).isEmpty());
    }

    @Test
    void dataGeneratorWhitelistDrivesSelection(@TempDir Path tempDir) {
        TestPackHandler pack = new TestPackHandler(tempDir);
        TestEntry first = new TestEntry("first", true);
        TestEntry second = new TestEntry("second", false);
        DataGenerator generator = new DataGenerator(
            true,
            true,
            List.of(second.getKey(), ResourceLocation.fromNamespaceAndPath("croparia_test", "missing")),
            new Template("${id}.json"),
            DgRegistry.ofMap(linkedMap(first, second)),
            new Template("selected=${id}")
        );

        generator.generate(pack);

        assertTrue(pack.queryOwned(generator, "croparia_test:first.json").isEmpty());
        assertEquals("selected=croparia_test:second", pack.queryOwned(generator, "croparia_test:second.json").orElseThrow());
    }

    @Test
    void aggregatedGeneratorCollectsAndRendersOnGenerated(@TempDir Path tempDir) {
        TestPackHandler pack = new TestPackHandler(tempDir);
        TestEntry first = new TestEntry("first", true);
        TestEntry second = new TestEntry("second", true);
        AggregatedGenerator generator = new AggregatedGenerator(
            true,
            true,
            List.of(),
            new Template("lang/all.json"),
            DgRegistry.ofMap(linkedMap(first, second)),
            new Template("\"${id}\""),
            new Template("{\n${content}\n}")
        );

        generator.generate(pack);
        generator.onGenerated(pack);

        String output = pack.queryOwned(generator, "lang/all.json").orElseThrow();
        assertTrue(output.contains("\"croparia_test:first\""));
        assertTrue(output.contains("\"croparia_test:second\""));
        assertTrue(output.startsWith("{\n"));
        assertTrue(output.endsWith("\n}"));
    }

    @Test
    void langGeneratorBuildsPerLanguageFiles(@TempDir Path tempDir) {
        TestPackHandler pack = new TestPackHandler(tempDir);
        TestTranslatableEntry entry = new TestTranslatableEntry(
            "crop_apple",
            "item.croparia.crop_apple",
            Map.of("en_us", "Apple Crop", "zh_cn", "苹果作物")
        );
        LangGenerator generator = new LangGenerator(
            true,
            true,
            List.of(),
            new Template("lang/${lang}.json"),
            DgRegistry.ofMap(linkedMap(entry)),
            new Template("\"${translation_key}\": \"${translations.get(_lang)}\"")
        );

        generator.generate(pack);
        generator.onGenerated(pack);

        assertEquals("{\n  \"item.croparia.crop_apple\": \"Apple Crop\"\n}",
            pack.queryOwned(generator, "lang/en_us.json").orElseThrow());
        assertEquals("{\n  \"item.croparia.crop_apple\": \"苹果作物\"\n}",
            pack.queryOwned(generator, "lang/zh_cn.json").orElseThrow());
    }

    @Test
    void langGeneratorRejectsNonTranslatableEntries(@TempDir Path tempDir) {
        TestPackHandler pack = new TestPackHandler(tempDir);
        TestEntry plain = new TestEntry("plain", true);
        @SuppressWarnings("unchecked")
        DgRegistry<? extends TranslatableEntry> incompatibleRegistry =
            (DgRegistry<? extends TranslatableEntry>) (DgRegistry<?>) DgRegistry.ofMap(linkedMap(plain));
        LangGenerator generator = new LangGenerator(
            true,
            true,
            List.of(),
            new Template("${id}.json"),
            incompatibleRegistry,
            new Template("x")
        );

        assertThrows(com.google.gson.JsonParseException.class, () -> generator.generate(pack));
    }

    @SafeVarargs
    private static <E extends DgEntry> Map<ResourceLocation, E> linkedMap(E... entries) {
        Map<ResourceLocation, E> map = new LinkedHashMap<>();
        for (E entry : entries) {
            map.put(entry.getKey(), entry);
        }
        return map;
    }

    private static final class TestPackHandler extends PackHandler {
        private TestPackHandler(Path root) {
            super(
                ResourceLocation.fromNamespaceAndPath("croparia_test", "pack_" + UUID.randomUUID()),
                root,
                new JsonObject(),
                () -> false
            );
        }

        private Optional<String> queryOwned(DataGenerator owner, String path) {
            return this.occupy(owner, path).map(value -> {
                assertInstanceOf(String.class, value);
                return (String) value;
            });
        }
    }

    private record TestEntry(ResourceLocation key, boolean load) implements DgEntry {
        private TestEntry(String path, boolean load) {
            this(ResourceLocation.fromNamespaceAndPath("croparia_test", path), load);
        }

        @Override
        public ResourceLocation getKey() {
            return key;
        }

        @Override
        public boolean shouldLoad() {
            return load;
        }
    }

    private static final class TestTranslatableEntry implements TranslatableEntry {
        private final ResourceLocation key;
        private final String translationKey;
        private final Map<String, String> translations;

        private TestTranslatableEntry(String path, String translationKey, Map<String, String> translations) {
            this.key = ResourceLocation.fromNamespaceAndPath("croparia_test", path);
            this.translationKey = translationKey;
            this.translations = translations;
        }

        @Override
        public Collection<String> getLangs() {
            return new ArrayList<>(this.translations.keySet());
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

        @Override
        public @Nullable String translate(String lang) {
            return this.translations.get(lang);
        }

        @Override
        public Map<String, String> getTranslations() {
            return this.translations;
        }

        @Override
        public ResourceLocation getKey() {
            return key;
        }

        @Override
        public boolean shouldLoad() {
            return true;
        }
    }
}
