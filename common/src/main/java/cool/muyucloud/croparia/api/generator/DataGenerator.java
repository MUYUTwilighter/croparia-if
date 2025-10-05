package cool.muyucloud.croparia.api.generator;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.generator.util.DgListener;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.placeholder.Template;
import cool.muyucloud.croparia.util.Dependencies;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Basic data generator, used to generate "per element" files like recipes, loot tables, etc.</p>
 * <p>To generate aggregated files like lang, tags, etc. use {@link LangGenerator} or {@link AggregatedGenerator}.</p>
 */
@SuppressWarnings("unused")
public class DataGenerator implements DgListener {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, MapCodec<? extends DataGenerator>> REGISTRY = new HashMap<>();

    /**
     * Registers a data generator codec, used to get Generator API known of custom data generator types.
     *
     * @param id    the id of the data generator
     * @param codec the codec of the data generator
     * @param <G>   the type of the data generator
     * @param <C>   the type of the codec
     * @return the codec
     */
    public static <G extends DataGenerator, C extends MapCodec<G>> C register(ResourceLocation id, C codec) {
        REGISTRY.put(id, codec);
        return codec;
    }

    /**
     * Read a data generator from a file, and classifies it based on the {@code @type} (optional, default to {@code croparia:generator}) meta tag.
     *
     * @param file the file to read
     * @return the read data generator
     */
    public static Optional<DataGenerator> read(File file) {
        try {
            JsonObject json = DgReader.read(file);
            JsonElement rawType = json.get("type");
            ResourceLocation type = ResourceLocation.tryParse(rawType == null ? "croparia:generator" : rawType.getAsString());
            JsonElement rawDependencies = json.get("dependencies");
            if (rawDependencies != null) {
                if (!CodecUtil.decodeJson(rawDependencies, Dependencies.CODEC).mapOrElse(Dependencies::available, err -> {
                    LOGGER.error("Failed to read generator {} due to invalid dependencies: {}", file, err.message());
                    return false;
                })) {
                    LOGGER.debug("Skipped loading data generator {} due to missing dependencies", file);
                    return Optional.empty();
                }
            }
            MapCodec<? extends DataGenerator> codec = REGISTRY.get(type);
            if (codec == null) {
                LOGGER.error("Unknown data generator type {} in file {}", type, file);
                return Optional.empty();
            }
            return CodecUtil.decodeJson(json, codec).mapOrElse(Optional::of, err -> {
                LOGGER.error("Failed to parse data generator {}: {}", file, err.message());
                return Optional.empty();
            });
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to read data generator from file " + file, e);
            return Optional.empty();
        }
    }

    public static final MapCodec<DataGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled").forGetter(DataGenerator::optionalEnabled),
        Codec.BOOL.optionalFieldOf("startup").forGetter(DataGenerator::optionalStartup),
        ResourceLocation.CODEC.listOf().optionalFieldOf("whitelist").forGetter(DataGenerator::optionalWhitelist),
        Template.CODEC.fieldOf("path").forGetter(DataGenerator::getPath),
        DgRegistry.CODEC.fieldOf("registry").forGetter(DataGenerator::getRegistry),
        Template.CODEC.fieldOf("template").forGetter(DataGenerator::getTemplate)
    ).apply(instance, (enabled, startup, whitelist, path, registry, template) -> new DataGenerator(
        enabled.orElse(true), startup.orElse(false), whitelist.orElse(List.of()), path, registry, template
    )));

    private final boolean enabled;
    private final boolean startup;
    private final List<ResourceLocation> whitelist;
    private final Template path;
    private final DgRegistry<? extends DgEntry> registry;
    private final Template template;

    public DataGenerator(boolean enabled, boolean startup, List<ResourceLocation> whitelist,
                         Template path, DgRegistry<? extends DgEntry> registry, Template template) {
        this.enabled = enabled;
        this.startup = startup;
        this.whitelist = whitelist instanceof ImmutableList<ResourceLocation> immutable ? immutable : ImmutableList.copyOf(whitelist);
        this.path = path;
        this.registry = registry;
        this.template = template;
    }

    /**
     * Whether meta tag {@code @enabled} is set to {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    public Optional<Boolean> optionalEnabled() {
        return this.isEnabled() ? Optional.empty() : Optional.of(false);
    }

    /**
     * Whether the generator should be loaded before server is started.
     */
    public boolean isStartup() {
        return startup;
    }

    public Optional<Boolean> optionalStartup() {
        return this.isStartup() ? Optional.of(true) : Optional.empty();
    }

    public List<ResourceLocation> getWhitelist() {
        return whitelist;
    }

    public Optional<List<ResourceLocation>> optionalWhitelist() {
        return this.getWhitelist().isEmpty() ? Optional.empty() : Optional.of(this.getWhitelist());
    }

    public Template getPath() {
        return path;
    }

    public String getPath(DgEntry entry) {
        return this.getPath().parse(entry);
    }

    public DgRegistry<? extends DgEntry> getRegistry() {
        return registry;
    }

    public Template getTemplate() {
        return template;
    }

    public String getTemplate(DgEntry entry) {
        return this.getTemplate().parse(entry);
    }

    public void generate(PackHandler pack) {
        if (this.isStartup() || CropariaIf.isServerStarted()) {
            if (this.getWhitelist().isEmpty()) {
                for (DgEntry entry : this.getRegistry()) {
                    if (entry.shouldLoad()) {
                        this.generate(entry, pack);
                    }
                }
            } else {
                for (ResourceLocation id : this.getWhitelist()) {
                    this.getRegistry().forName(id).ifPresent(e -> this.generate(e, pack));
                }
            }
        }
    }

    protected void generate(DgEntry entry, PackHandler pack) {
        String relative = this.getPath(entry);
        String replaced = this.getTemplate(entry);
        pack.cache(relative, replaced, this);
    }
}
