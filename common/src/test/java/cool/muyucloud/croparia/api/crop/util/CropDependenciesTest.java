package cool.muyucloud.croparia.api.crop.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropDependenciesTest {
    @Test
    void shouldLoadUsesAnyCandidateAndTreatsEmptyAsLoadable() {
        TestCropDependencies satisfiable = new TestCropDependencies(
            Map.of("missing_mod", "k1", "loaded_mod", "k2"),
            Set.of("loaded_mod")
        );
        assertTrue(satisfiable.shouldLoad());

        TestCropDependencies unsatisfied = new TestCropDependencies(
            Map.of("missing_mod", "k1"),
            Set.of()
        );
        assertFalse(unsatisfied.shouldLoad());

        assertTrue(new TestCropDependencies(Map.of(), Set.of()).shouldLoad());
    }

    @Test
    void chosenReturnsFirstAvailableAndCachesResult() {
        LinkedHashMap<String, String> candidates = new LinkedHashMap<>();
        candidates.put("mod_a", "key.a");
        candidates.put("mod_b", "key.b");
        MutableAvailabilityCropDependencies deps = new MutableAvailabilityCropDependencies(candidates, Set.of("mod_b"));

        assertEquals("key.b", deps.getChosen());

        deps.setLoaded(Set.of("mod_a"));
        assertEquals("key.b", deps.getChosen());
    }

    @Test
    void gettersSizeAndVarargValidationWork() {
        CropDependencies deps = new CropDependencies("mod_a", "key.a", "mod_b", "key.b");
        assertEquals(2, deps.size());
        assertEquals("key.a", deps.getKey("mod_a"));
        assertNull(deps.getKey("missing"));
        assertFalse(deps.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new CropDependencies("mod_a", "key.a", "orphan"));
    }

    private static class TestCropDependencies extends CropDependencies {
        private final Set<String> loaded;

        private TestCropDependencies(Map<String, String> candidates, Set<String> loaded) {
            super(candidates);
            this.loaded = loaded;
        }

        @Override
        protected boolean shouldLoad(String modId) {
            return loaded.contains(modId);
        }
    }

    private static class MutableAvailabilityCropDependencies extends CropDependencies {
        private Set<String> loaded;

        private MutableAvailabilityCropDependencies(Map<String, String> candidates, Set<String> loaded) {
            super(candidates);
            this.loaded = loaded;
        }

        private void setLoaded(Set<String> loaded) {
            this.loaded = loaded;
        }

        @Override
        protected boolean shouldLoad(String modId) {
            return loaded.contains(modId);
        }
    }
}
