package cool.muyucloud.croparia.api.crop;

import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCrop<T> implements TranslatableEntry {
    public static String defaultTranslation(ResourceLocation id) {
        String name = id.getPath();
        name = name.replaceAll("_", " ").trim();
        StringBuilder builder = new StringBuilder();
        for (String token : name.split(" ")) {
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1)).append(" ");
        }
        return builder.toString().trim();
    }

    public abstract @NotNull Material<T> getMaterial();

    public abstract void onRegister();
}
