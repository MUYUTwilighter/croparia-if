package cool.muyucloud.croparia.api.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgElement;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 **/
public class AggregatedGenerator extends DataGenerator {
    public static final MapCodec<AggregatedGenerator> CODEC = CodecUtil.extend(
        DataGenerator.CODEC,
        Codec.STRING.fieldOf("content").forGetter(AggregatedGenerator::getContent),
        (base, content) -> new AggregatedGenerator(
            base.isEnabled(), base.isStartup(), base.getWhitelist(),
            base.getPath(), base.getRegistry(), content, base.getTemplate()
        )
    );

    private final String content;
    protected final transient Map<String, List<String>> cache = new HashMap<>();

    public AggregatedGenerator(boolean enabled, boolean startup, List<ResourceLocation> whitelist, String path, DgRegistry<? extends DgElement> iterable, String content, String template) {
        super(enabled, startup, whitelist, path, iterable, template);
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getContent(DgElement element) {
        return replace(this.getContent(), element);
    }

    @Override
    public String getTemplate(DgElement element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void generate(PackHandler pack) {
        super.generate(pack);
        for (Map.Entry<String, List<String>> entry : this.cache.entrySet()) {
            String relative = entry.getKey();
            StringBuilder builder = new StringBuilder();
            for (String s : entry.getValue()) {
                builder.append(s).append(",\n");
            }
            String content = builder.isEmpty() ? "" : builder.substring(0, builder.length() - 2);
            pack.addFile(relative, this.getTemplate().replaceAll("\\{content}", content));
        }
        this.cache.clear();
    }

    @Override
    protected void generate(DgElement element, PackHandler pack) {
        List<String> list = this.cache.computeIfAbsent(this.getPath(element), k -> new LinkedList<>());
        list.add(this.getContent(element));
    }
}
