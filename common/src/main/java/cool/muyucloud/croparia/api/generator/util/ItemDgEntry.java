package cool.muyucloud.croparia.api.generator.util;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record ItemDgEntry(@NonNull Identifier key) implements DgEntry {
    @Override
    public boolean shouldLoad() {
        return true;
    }

    @Override
    public @NonNull Identifier getKey() {
        return this.key;
    }
}
