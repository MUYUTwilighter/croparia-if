package cool.muyucloud.croparia.api.generator.util;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ItemDgEntry(@NotNull Identifier key) implements DgEntry {
    @Override
    public boolean shouldLoad() {
        return true;
    }

    @Override
    public @NotNull Identifier getKey() {
        return this.key;
    }
}
