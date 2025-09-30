package cool.muyucloud.croparia.api.generator;

import com.google.gson.JsonParseException;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.Template;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


public class LangGenerator extends DataGenerator {
    public static final MapCodec<LangGenerator> CODEC = DataGenerator.CODEC.xmap(dg -> {
        try {
            @SuppressWarnings("unchecked")
            DgRegistry<? extends TranslatableEntry> translatable = (DgRegistry<? extends TranslatableEntry>) dg.getRegistry();
            for (TranslatableEntry element : translatable) {
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
        boolean enabled, boolean startup, List<ResourceLocation> whitelist, Template path,
        DgRegistry<? extends TranslatableEntry> registry, Template template
    ) {
        super(enabled, startup, whitelist, path, registry, template);
    }

    @Override
    public void onGenerated(PackHandler handler) {
        TRANSLATIONS.forEach((path, translations) -> {
            StringBuilder builder = new StringBuilder("{\n");
            translations.forEach(translation -> builder.append("  ").append(translation).append(",\n"));
            String generated = builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2);
            generated += "\n}";
            handler.cache(path, generated);
        });
        TRANSLATIONS.clear();
    }

    @Override
    protected void generate(DgEntry entry, PackHandler pack) {
        if (entry instanceof TranslatableEntry translatable) {
            AtomicReference<String> langRef = new AtomicReference<>();
            @SuppressWarnings("unchecked")
            Placeholder<TranslatableEntry> parser = Placeholder.build(builder -> builder
                .then(PatternKey.literal("lang"), TypeMapper.of(e -> langRef.get()), Placeholder.STRING)
                .overwrite((Placeholder<TranslatableEntry>) translatable.placeholder(), TypeMapper.identity()));
            Function<String, String> preProcess = placeholder -> placeholder.replaceAll("_lang", langRef.get());
            for (String lang : translatable.getLangs()) {
                langRef.set(lang);
                String relative = this.getPath().parse(translatable, parser, preProcess);
                List<String> list = TRANSLATIONS.computeIfAbsent(relative, k -> new LinkedList<>());
                list.add(this.getTemplate().parse(translatable, parser, preProcess));
            }
        } else {
            throw new JsonParseException("Entry %s in %s is not translatable".formatted(entry.getKey(), this.getRegistry().getId()));
        }
    }
}
