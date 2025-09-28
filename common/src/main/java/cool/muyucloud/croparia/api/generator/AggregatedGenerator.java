package cool.muyucloud.croparia.api.generator;

import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.PlaceholderAccess;
import cool.muyucloud.croparia.api.placeholder.Template;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 **/
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
    protected static final Map<String, List<String>> CACHE = new HashMap<>();

    private final Template content;

    public AggregatedGenerator(boolean enabled, boolean startup, List<ResourceLocation> whitelist, Template path, DgRegistry<? extends DgEntry> iterable, Template content, Template template) {
        super(enabled, startup, whitelist, path, iterable, template);
        this.content = content;
    }

    public Template getContent() {
        return content;
    }

    public String getContent(DgEntry entry) {
        return this.getContent().replace(entry);
    }

    @Override
    public String getTemplate(DgEntry entry) {
        throw new UnsupportedOperationException("AggregatedGenerator does not support single file generation.");
    }

    @Override
    public void generate(PackHandler pack) {
        super.generate(pack);
    }

    @Override
    protected void generate(DgEntry entry, PackHandler pack) {
        List<String> list = CACHE.computeIfAbsent(this.getPath(entry), k -> new LinkedList<>());
        list.add(this.getContent(entry));
    }

    @Override
    public void onGenerated(PackHandler handler) {
        for (var entry : CACHE.entrySet()) {
            String relative = entry.getKey();
            StringBuilder builder = new StringBuilder();
            for (String s : entry.getValue()) {
                builder.append(s).append(",\n");
            }
            String content = builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2);
            handler.cache(relative, this.getTemplate().replace(PlaceholderAccess.of(content, CONTENT_PLACEHOLDER)));
        }
        CACHE.clear();
    }
}
