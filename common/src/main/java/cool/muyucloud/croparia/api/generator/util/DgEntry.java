package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.PlaceholderAccess;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface DgEntry extends PlaceholderAccess {
    Placeholder<DgEntry> PLACEHOLDER = Placeholder.build(node -> node.then(
        PatternKey.literal("id"), TypeMapper.of(DgEntry::getKey), Placeholder.ID
    ));

    @NotNull
    ResourceLocation getKey();

    default Placeholder<? extends DgEntry> placeholder() {
        return PLACEHOLDER;
    }

    boolean shouldLoad();
}
