package cool.muyucloud.croparia.api.generator;

import com.google.gson.JsonParseException;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.TranslatableEntry;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.Template;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    public static final ResourceLocation TYPE = CropariaIf.of("lang");

    public LangGenerator(
        boolean enabled, boolean startup, List<ResourceLocation> whitelist, Template path,
        DgRegistry<? extends TranslatableEntry> registry, Template template
    ) {
        super(enabled, startup, whitelist, path, registry, template);
    }

    @Override
    public void onGenerated(PackHandler handler) {
        handler.getAll(this).forEach(entry -> {
            StringBuilder builder = new StringBuilder();
            if (entry.value() instanceof Collection<?> translations) {
                translations.forEach(translation -> builder.append("  ").append(translation.toString()).append(",\n"));
                String generated = "{\n" + (builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2)) + "\n}";
                handler.cache(entry.path(), generated, this);
            }
        });
    }

    @Override
    protected void generate(DgEntry entry, PackHandler pack) {
        if (entry instanceof TranslatableEntry translatable) {
            AtomicReference<String> langRef = new AtomicReference<>();
            @SuppressWarnings("unchecked")
            Placeholder<DgEntry> parser = Placeholder.build(builder -> builder
                .then(PatternKey.literal("lang"), TypeMapper.of(e -> langRef.get()), Placeholder.STRING)
                .overwrite((Placeholder<DgEntry>) entry.placeholder(), TypeMapper.identity()));
            Function<String, String> preProcess = placeholder -> placeholder.replaceAll("_lang", langRef.get());
            for (String lang : translatable.getLangs()) {
                langRef.set(lang);
                String path = this.getPath().parse(entry, parser, preProcess);
                @SuppressWarnings("unchecked")
                Collection<Object> translations = pack.occupy(this, path).map(value -> {
                    if (value instanceof Collection<?> collection) {
                        return (Collection<Object>) collection;
                    } else {
                        return null;
                    }
                }).orElseGet(() -> pack.cache(path, new ArrayList<>(), this));
                translations.add(this.getTemplate().parse(entry, parser, preProcess));
            }
        } else {
            throw new JsonParseException("Entry %s is not translatable".formatted(entry));
        }
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
