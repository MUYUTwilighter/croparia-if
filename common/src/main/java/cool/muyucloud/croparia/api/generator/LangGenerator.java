package cool.muyucloud.croparia.api.generator;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgElement;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.TranslatableElement;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class LangGenerator extends DataGenerator {
    public static final MapCodec<LangGenerator> CODEC = DataGenerator.CODEC.xmap(dg -> {
        try {
            @SuppressWarnings("unchecked")
            DgRegistry<? extends TranslatableElement> translatable = (DgRegistry<? extends TranslatableElement>) dg.getRegistry();
            for (TranslatableElement element : translatable) {
                element.translate("en_us");
                break;
            }
            return new LangGenerator(
                dg.isEnabled(), dg.isStartup(), dg.getWhitelist(),
                dg.getPath(), translatable, dg.getTemplate()
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Iterable %s is not translatable".formatted(dg.getRegistry()), e);
        }
    }, lg -> lg);

    private static final Map<String, List<String>> TRANSLATIONS = new HashMap<>();

    public LangGenerator(
        boolean enabled, boolean startup, List<ResourceLocation> whitelist, String path,
        DgRegistry<? extends TranslatableElement> registry, String template
    ) {
        super(enabled, startup, whitelist, path, registry, template);
    }

    @Override
    public String getPath() {
        return super.getPath();
    }

    @Override
    public void onGenerated(PackHandler handler) {
        TRANSLATIONS.forEach((path, translations) -> {
            StringBuilder builder = new StringBuilder("{\n");
            translations.forEach(translation -> builder.append("  ").append(translation).append(",\n"));
            String generated = builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2);
            generated += "\n}";
            handler.addFile(path, generated);
        });
        TRANSLATIONS.clear();
    }

    @Override
    protected void generate(DgElement element, PackHandler pack) {
        if (element instanceof TranslatableElement translatable) {
            for (String lang : translatable.getLangs()) {
                String relative = replace(this.getPath().replaceAll("\\{lang}", lang), element);
                List<String> list = TRANSLATIONS.computeIfAbsent(relative, k -> new LinkedList<>());
                list.add(replace(this.getTemplate().replaceAll("\\{lang}", lang), element));
            }
        } else {
            throw new IllegalArgumentException("Element %s is not translatable".formatted(element.getKey()));
        }
    }
}
