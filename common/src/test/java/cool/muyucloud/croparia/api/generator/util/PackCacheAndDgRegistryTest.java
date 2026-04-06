package cool.muyucloud.croparia.api.generator.util;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PackCacheAndDgRegistryTest {
    @Test
    void mapRegistryAndEnumRegistryLookupByName() {
        DummyEntry one = new DummyEntry("one");
        DummyEntry two = new DummyEntry("two");
        DgRegistry<DummyEntry> mapRegistry = DgRegistry.ofMap(Map.of(one.getKey(), one, two.getKey(), two));

        assertEquals(Optional.of(one), mapRegistry.forName(one.getKey()));
        assertFalse(mapRegistry.forName(ResourceLocation.fromNamespaceAndPath("croparia_test", "none")).isPresent());

        DgRegistry<DummyEnumEntry> enumRegistry = DgRegistry.ofEnum(DummyEnumEntry.class);
        assertTrue(enumRegistry.forName(DummyEnumEntry.A.getKey()).isPresent());
        assertTrue(enumRegistry.forName(DummyEnumEntry.B.getKey()).isPresent());
    }

    @Test
    void registryCodecRoundTripUsesRegisteredInstance() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("croparia_test", "registry_" + UUID.randomUUID());
        DgRegistry<DummyEntry> registry = DgRegistry.ofMap(Map.of());
        DgRegistry.register(id, registry);

        var encoded = DgRegistry.CODEC.encodeStart(JsonOps.INSTANCE, registry).getOrThrow();
        DgRegistry<? extends DgEntry> decoded = DgRegistry.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertSame(registry, decoded);
    }

    @Test
    void getIdReturnsRegisteredId() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("croparia_test", "registry_" + UUID.randomUUID());
        DgRegistry<DummyEntry> registry = DgRegistry.ofMap(Map.of());
        DgRegistry.register(id, registry);
        assertEquals(id, registry.getId());
    }

    private record DummyEntry(ResourceLocation key) implements DgEntry {
        DummyEntry(String path) {
            this(ResourceLocation.fromNamespaceAndPath("croparia_test", path));
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

    private enum DummyEnumEntry implements DgEntry {
        A("a"), B("b");

        private final ResourceLocation key;

        DummyEnumEntry(String path) {
            this.key = ResourceLocation.fromNamespaceAndPath("croparia_test", "enum_" + path);
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
