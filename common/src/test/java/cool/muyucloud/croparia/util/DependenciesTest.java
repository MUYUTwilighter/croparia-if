package cool.muyucloud.croparia.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependenciesTest {
    @Test
    void emptyAndAvailabilitySemanticsFollowAnyAndAllRules() {
        TestDependencies deps = new TestDependencies(
            List.of(
                List.of("a", "b"),
                List.of("c")
            ),
            Set.of("b", "c")
        );
        assertTrue(deps.available());

        TestDependencies unsatisfied = new TestDependencies(
            List.of(
                List.of("a", "b"),
                List.of("missing")
            ),
            Set.of("b")
        );
        assertFalse(unsatisfied.available());

        TestDependencies empty = new TestDependencies(List.of(List.of(), List.of()), Set.of());
        assertTrue(empty.isEmpty());
    }

    private static class TestDependencies extends Dependencies {
        private final Set<String> loaded;

        TestDependencies(List<List<String>> dependencies, Set<String> loaded) {
            super(dependencies);
            this.loaded = loaded;
        }

        @Override
        protected boolean available(String e) {
            return loaded.contains(e);
        }
    }
}

