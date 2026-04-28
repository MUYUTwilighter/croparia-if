package cool.muyucloud.croparia.api.json.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTransformerImplFabricTest {
    @Test
    void transformsToml() {
        JsonObject json = withTransformerClassLoader(() -> JsonTransformerImpl.transformToml("name = \"croparia\"").getAsJsonObject());
        assertEquals("croparia", json.get("name").getAsString());
    }

    @Test
    void invalidTomlThrowsJsonParseException() {
        assertThrows(JsonParseException.class, () -> withTransformerClassLoader(() -> JsonTransformerImpl.transformToml("name = ")));
    }

    private static <T> T withTransformerClassLoader(ThrowingSupplier<T> supplier) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(JsonTransformerImpl.class.getClassLoader());
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
