package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.placeholder.Template;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TranslatableAndPackCacheEntryTest {
    @Test
    void translatableEntryPlaceholderExposesIdAndTranslations() {
        TranslatableEntry entry = new DummyTranslatableEntry(
            Identifier.fromNamespaceAndPath("croparia_test", "sample"),
            Map.of("en_us", "Apple", "zh_cn", "PingGuo")
        );

        Template template = new Template("${id}|${translation_key}|${translations.get(en_us)}|${translations.get(zh_cn)}");
        String parsed = template.parse(entry);
        assertEquals("croparia_test:sample|croparia_test.sample|Apple|PingGuo", parsed);
    }

    @Test
    void packCacheEntryEqualityDependsOnPathOnly() {
        PackCacheEntry<String> a = new PackCacheEntry<>(null, "path/a.json", "v1");
        PackCacheEntry<String> b = new PackCacheEntry<>(null, "path/a.json", "v2");
        PackCacheEntry<String> c = new PackCacheEntry<>(null, "path/c.json", "v1");

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertTrue(a.getCache().isPresent());
        assertEquals("v1", a.getCache().orElseThrow());
    }

    private record DummyTranslatableEntry(Identifier key, Map<String, String> translations) implements TranslatableEntry {
        @Override
        public Identifier getKey() {
            return key;
        }

        @Override
        public Collection<String> getLangs() {
            return translations.keySet();
        }

        @Override
        public String getTranslationKey() {
            return key.getNamespace() + "." + key.getPath();
        }

        @Override
        public String translate(String lang) {
            return translations.get(lang);
        }

        @Override
        public Map<String, String> getTranslations() {
            return translations;
        }

        @Override
        public boolean shouldLoad() {
            return true;
        }
    }
}
