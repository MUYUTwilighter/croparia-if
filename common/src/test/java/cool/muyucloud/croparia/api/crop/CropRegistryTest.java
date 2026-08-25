package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.crop.util.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CropRegistryTest {
    @Test
    void registerTriggersOnRegisterOnlyWhenTransitioningToLoaded(@TempDir Path tempDir) {
        CropRegistry<DummyCrop> registry = new CropRegistry<>(tempDir, DummyCrop.CODEC);
        Identifier id = Identifier.fromNamespaceAndPath("croparia_test", "alpha");

        DummyCrop firstLoaded = new DummyCrop(id, true, "k", Map.of("en_us", "A"));
        DummyCrop secondLoaded = new DummyCrop(id, true, "k", Map.of("en_us", "B"));
        DummyCrop unloaded = new DummyCrop(id, false, "k", Map.of("en_us", "C"));
        DummyCrop loadedAgain = new DummyCrop(id, true, "k", Map.of("en_us", "D"));

        registry.register(firstLoaded);
        registry.register(secondLoaded);
        registry.register(unloaded);
        registry.register(loadedAgain);

        assertEquals(1, firstLoaded.registeredCount());
        assertEquals(0, secondLoaded.registeredCount());
        assertEquals(1, loadedAgain.registeredCount());
        assertSame(loadedAgain, registry.forName(id).orElseThrow());

        List<DummyCrop> loaded = registryLoaded(registry);
        assertEquals(1, loaded.size());
        assertSame(loadedAgain, loaded.get(0));
    }

    @Test
    void readCropsReadsJsonRecursivelyAndIgnoresNonJsonFiles(@TempDir Path tempDir) throws IOException {
        CropRegistry<DummyCrop> registry = new CropRegistry<>(tempDir, DummyCrop.CODEC);
        Files.writeString(tempDir.resolve("a.json"),
            "{\"id\":\"croparia_test:a\",\"load\":true,\"translation_key\":\"ta\",\"translations\":{\"en_us\":\"A\"}}",
            StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("ignored.txt"), "{\"id\":\"croparia_test:x\"}", StandardCharsets.UTF_8);
        Files.createDirectories(tempDir.resolve("nested"));
        Files.writeString(tempDir.resolve("nested/b.json"),
            "{\"id\":\"croparia_test:b\",\"load\":false,\"translation_key\":\"tb\",\"translations\":{\"en_us\":\"B\"}}",
            StandardCharsets.UTF_8);

        registry.readCrops();

        assertEquals(2, registry.size());
        assertTrue(registry.exists(Identifier.fromNamespaceAndPath("croparia_test", "a")));
        assertTrue(registry.exists(Identifier.fromNamespaceAndPath("croparia_test", "b")));
        assertFalse(registry.exists(Identifier.fromNamespaceAndPath("croparia_test", "x")));

        List<String> loadedIds = registryLoaded(registry).stream()
            .map(c -> c.getKey().toString())
            .collect(Collectors.toList());
        assertEquals(List.of("croparia_test:a"), loadedIds);
    }

    @Test
    void readCropsCreatesDirectoryAndSkipsMalformedJson(@TempDir Path tempDir) throws IOException {
        Path missingDir = tempDir.resolve("missing_registry_root");
        CropRegistry<DummyCrop> registry = new CropRegistry<>(missingDir, DummyCrop.CODEC);
        assertFalse(Files.exists(missingDir));

        registry.readCrops();
        assertTrue(Files.exists(missingDir));
        assertEquals(0, registry.size());

        Files.writeString(missingDir.resolve("broken.json"), "{\"id\":", StandardCharsets.UTF_8);
        Files.writeString(missingDir.resolve("valid.json"),
            "{\"id\":\"croparia_test:ok\",\"load\":true,\"translation_key\":\"k\",\"translations\":{\"en_us\":\"OK\"}}",
            StandardCharsets.UTF_8);
        registry.readCrops();

        assertEquals(1, registry.size());
        assertTrue(registry.exists(Identifier.fromNamespaceAndPath("croparia_test", "ok")));
    }

    @Test
    void dumpCropWritesNamespacedPath(@TempDir Path tempDir) throws IOException {
        CropRegistry<DummyCrop> registry = new CropRegistry<>(tempDir, DummyCrop.CODEC);
        DummyCrop crop = new DummyCrop(
            Identifier.fromNamespaceAndPath("croparia_test", "dump_target"),
            true,
            "key.dump",
            Map.of("en_us", "Dump Target")
        );

        Path dumped = registry.dumpCrop(crop);
        assertEquals(tempDir.resolve("croparia_test/dump_target.json"), dumped);
        assertTrue(Files.exists(dumped));

        String content = Files.readString(dumped, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"id\": \"croparia_test:dump_target\""));
        assertTrue(content.contains("\"translation_key\": \"key.dump\""));
    }

    @Test
    void dumpCropsWritesAllRegisteredCrops(@TempDir Path tempDir) throws IOException {
        CropRegistry<DummyCrop> registry = new CropRegistry<>(tempDir, DummyCrop.CODEC);
        DummyCrop a = new DummyCrop(Identifier.fromNamespaceAndPath("croparia_test", "a"), true, "k.a", Map.of("en_us", "A"));
        DummyCrop b = new DummyCrop(Identifier.fromNamespaceAndPath("croparia_test", "b"), false, "k.b", Map.of("en_us", "B"));
        registry.register(a);
        registry.register(b);

        registry.dumpCrops();

        assertTrue(Files.exists(tempDir.resolve("croparia_test/a.json")));
        assertTrue(Files.exists(tempDir.resolve("croparia_test/b.json")));
    }

    private static List<DummyCrop> registryLoaded(CropRegistry<DummyCrop> registry) {
        java.util.ArrayList<DummyCrop> loaded = new java.util.ArrayList<>();
        registry.forLoaded(loaded::add);
        return loaded;
    }

    private static final class DummyCrop extends AbstractCrop<Object> {
        private static final Codec<DummyCrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(DummyCrop::getKey),
            Codec.BOOL.optionalFieldOf("load", true).forGetter(DummyCrop::shouldLoad),
            Codec.STRING.optionalFieldOf("translation_key", "dummy.translation").forGetter(DummyCrop::getTranslationKey),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations", Map.of("en_us", "Dummy")).forGetter(DummyCrop::getTranslations)
        ).apply(instance, DummyCrop::new));

        private static final Material<Object> MATERIAL = new Material<>("minecraft:air", 1) {
            @Override
            public @NonNull List<Object> candidates() {
                return List.of();
            }

            @Override
            public @NonNull ItemStack asItem() {
                return null;
            }

            @Override
            public @NonNull List<ItemStack> asItems() {
                return List.of();
            }
        };

        private final Identifier id;
        private final boolean load;
        private final String translationKey;
        private final ImmutableMap<String, String> translations;
        private final AtomicInteger registerCalls = new AtomicInteger(0);

        private DummyCrop(Identifier id, boolean load, String translationKey, Map<String, String> translations) {
            this.id = id;
            this.load = load;
            this.translationKey = translationKey;
            this.translations = ImmutableMap.copyOf(translations);
        }

        @Override
        public @NotNull Identifier getKey() {
            return id;
        }

        @Override
        public boolean shouldLoad() {
            return load;
        }

        @Override
        public Collection<String> getLangs() {
            return translations.keySet();
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

        @Override
        public @Nullable String translate(String lang) {
            return translations.get(lang);
        }

        @Override
        public Map<String, String> getTranslations() {
            return translations;
        }

        @Override
        public @NotNull Material<Object> getMaterial() {
            return MATERIAL;
        }

        @Override
        public void onRegister() {
            registerCalls.incrementAndGet();
        }

        private int registeredCount() {
            return registerCalls.get();
        }
    }
}
