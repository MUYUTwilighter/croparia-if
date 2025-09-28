package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.PlaceholderAccess;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface DgEntry extends PlaceholderAccess {
    Placeholder<DgEntry> PLACEHOLDER = Placeholder.build(node -> node.then(
        Pattern.compile("^id$|^key$"), DgEntry::getKey, Placeholder.ID
    ));

    @NotNull
    ResourceLocation getKey();

    default Placeholder<? extends DgEntry> placeholder() {
        return PLACEHOLDER;
    }

    boolean shouldLoad();
}
