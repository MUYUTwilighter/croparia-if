package cool.muyucloud.croparia.api.generator;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.PackCacheEntry;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.Template;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AggregatedGenerator extends DataGenerator {
    public static final MapCodec<AggregatedGenerator> CODEC = CodecUtil.extend(
        DataGenerator.CODEC,
        Template.CODEC.fieldOf("content").forGetter(AggregatedGenerator::getContent),
        (base, content) -> new AggregatedGenerator(
            base.isEnabled(), base.isStartup(), base.getWhitelist(),
            base.getPath(), base.getRegistry(), content, base.getTemplate()
        )
    );
    public static final Placeholder<String> CONTENT_PLACEHOLDER = Placeholder.build(builder -> builder
        .then(Pattern.compile("^content$"), Placeholder.STRING));

    private final Template content;

    public AggregatedGenerator(boolean enabled, boolean startup, List<ResourceLocation> whitelist, Template path, DgRegistry<? extends DgEntry> iterable, Template content, Template template) {
        super(enabled, startup, whitelist, path, iterable, template);
        this.content = content;
    }

    public Template getContent() {
        return content;
    }

    public String getContent(DgEntry entry) {
        return this.getContent().parse(entry);
    }

    @Override
    public String getTemplate(DgEntry entry) {
        throw new UnsupportedOperationException("AggregatedGenerator does not support single file generation.");
    }

    @Override
    protected void generate(DgEntry entry, PackHandler pack) {
        String path = this.getPath(entry);
        @SuppressWarnings("unchecked")
        Collection<Object> cache = pack.occupy(this, path).map(value -> {
            if (value instanceof Collection<?> collection) {
                return (Collection<Object>) collection;
            } else {
                return null;
            }
        }).orElseGet(() -> pack.cache(path, new ArrayList<>(), this));
        cache.add(this.getContent(entry));
    }

    @Override
    public void onGenerated(PackHandler handler) {
        Set<PackCacheEntry<?>> caches = handler.getAll(this);
        for (PackCacheEntry<?> entry : caches) {
            StringBuilder builder = new StringBuilder();
            if (entry.value() instanceof Collection<?> collection) {
                for (Object s : collection) {
                    builder.append(s).append(",\n");
                }
            }
            String content = builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2);
            handler.cache(entry.path(), this.getTemplate().parse(content, CONTENT_PLACEHOLDER), this);
        }
    }
}
