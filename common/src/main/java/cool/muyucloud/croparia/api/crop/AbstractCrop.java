package cool.muyucloud.croparia.api.crop;

import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public abstract class AbstractCrop<T> implements TranslatableEntry {
    public static String defaultTranslation(Identifier id) {
        String name = id.getPath();
        name = name.replace("_", " ").trim();
        StringBuilder builder = new StringBuilder();
        for (String token : name.split(" ")) {
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1)).append(" ");
        }
        return builder.toString().trim();
    }

    public abstract @NonNull Material<T> getMaterial();

    public abstract void onRegister();
}
