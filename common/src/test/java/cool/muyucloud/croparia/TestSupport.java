package cool.muyucloud.croparia;

import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class TestSupport {
    private TestSupport() {
    }

    public static <T> T getOrThrow(DataResult<T> result) {
        return result.result().orElseThrow(() -> new AssertionError(result.error()
            .map(DataResult.PartialResult::message)
            .orElse("Unknown DataResult error")));
    }

    public static boolean isSuccess(DataResult<?> result) {
        return result.result().isPresent();
    }

    public static boolean isError(DataResult<?> result) {
        return result.error().isPresent();
    }

    public static ResourceLocation rl(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static <T> T first(List<T> list) {
        return list.get(0);
    }
}
