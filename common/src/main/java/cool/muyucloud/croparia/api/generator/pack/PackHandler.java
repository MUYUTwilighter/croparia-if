package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.util.JarJarEntry;
import cool.muyucloud.croparia.api.generator.util.PackCache;
import cool.muyucloud.croparia.api.generator.util.PackCacheEntry;
import cool.muyucloud.croparia.api.json.JsonTransformer;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A manager class for a data/resource pack represented by a directory.
 */
public abstract class PackHandler {
    public static final Gson GSON = new Gson();
    private static final Pattern PATTERN = Pattern.compile("^data-generators/([^/]+)/([^/]+)/[^/]+$");
    private static final LazySupplier<Map<ResourceLocation, Collection<JarJarEntry>>> BUILTIN_GENERATORS = LazySupplier.of(() -> {
        Map<ResourceLocation, Collection<JarJarEntry>> map = new HashMap<>();
        forEachJar((file, modId) -> {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(file)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        Matcher matcher = PATTERN.matcher(name);
                        if (matcher.find()) {
                            ResourceLocation id = ResourceLocation.tryBuild(matcher.group(1), matcher.group(2));
                            if (id == null) {
                                DataGenerator.LOGGER.error("Invalid generator entry \"%s\" in mod \"%s\"".formatted(name, modId));
                                continue;
                            }
                            Collection<JarJarEntry> collected = map.computeIfAbsent(id, k -> new ArrayList<>());
                            collected.add(new JarJarEntry(file, entry));
                        }
                    }
                } catch (IOException e) {
                    DataGenerator.LOGGER.error("Failed to read generators from mod \"%s\"".formatted(modId), e);
                }
            }
        });
        return map;
    });

    @ExpectPlatform
    public static void forEachJar(BiConsumer<File, String> consumer) {
        throw new NotImplementedException("Not implemented");
    }

    public static Collection<JarJarEntry> getBuiltinGenerators(ResourceLocation id) {
        return BUILTIN_GENERATORS.get().getOrDefault(id, List.of());
    }

    protected final ResourceLocation id;
    protected final Path root;
    protected final JsonObject meta;
    protected final transient Supplier<Boolean> override;
    protected final transient PackCache cache = new PackCache();
    protected final transient Map<String, DataGenerator> generators = new HashMap<>();

    public PackHandler(ResourceLocation id, Path path, JsonObject meta, Supplier<Boolean> override) {
        this.id = id;
        this.root = path;
        this.meta = meta;
        this.override = override;
        this.writeMeta();
    }

    public void clear() {
        Path path = this.getRoot().resolve(this.proxyPath("/"));
        File file = path.toFile();
        if (file.isDirectory()) {
            try {
                FileUtil.deleteUnder(file);
            } catch (IOException e) {
                CropariaIf.LOGGER.error("Failed to clear pack directory", e);
            }
        }
    }

    public boolean canOverride() {
        return this.override.get();
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public void onTriggered() {
        this.cache.clear(); // In case of exception during last generation
        this.generators.clear();
        if (this.canOverride()) {
            this.clear();
        }
        this.writeMeta();
        this.readBuiltinGenerators();
        this.readGenerators();
        this.generate();
        this.onGenerated();
        this.dump();
        this.onDumped();
        this.cache.clear();
    }

    protected void onGenerated() {
        this.generators.forEach((fn, generator) -> generator.onGenerated(this));
    }

    protected void onDumped() {
        this.generators.forEach((fn, generator) -> generator.onDumped(this));
    }

    protected void readBuiltinGenerators() {
        String prefix = "data-generators/%s/%s/".formatted(this.getId().getNamespace(), this.getId().getPath());
        getBuiltinGenerators(this.getId()).forEach(entry -> {
            String name = entry.getJarEntry().getName().substring(prefix.length());
            try {
                entry.forInputStream(input -> {
                    String content = new String(input.readAllBytes());
                    JsonElement json = JsonTransformer.transform(content, name);
                    if (!json.isJsonObject())
                        throw new JsonParseException("Generator file is not a JSON object: " + name);
                    Optional<DataGenerator> mayGenerator = DataGenerator.read(json.getAsJsonObject());
                    if (mayGenerator.isPresent()) {
                        DataGenerator generator = mayGenerator.get();
                        generator.setName(name);
                        this.generators.put(name, generator);
                    }
                });
            } catch (IOException | JsonParseException | IllegalStateException e) {
                DataGenerator.LOGGER.error("Failed to read builtin generator \"%s\" from mod jar \"%s\"".formatted(name, entry.getJarFile().getName()), e);
            }
        });
    }

    protected void readGenerators() {
        File parent = this.getRoot().resolve("generators").toFile();
        try {
            FileUtil.forFilesIn(parent, file -> {
                String name = file.getAbsolutePath().substring(parent.getAbsolutePath().length() + 1);
                try (FileInputStream fis = new FileInputStream(file)) {
                    String content = new String(fis.readAllBytes());
                    JsonElement json = JsonTransformer.transform(content, name);
                    if (!json.isJsonObject())
                        throw new JsonParseException("Generator file is not a JSON object: " + name);
                    Optional<DataGenerator> mayGenerator = DataGenerator.read(json.getAsJsonObject());
                    if (mayGenerator.isPresent()) {
                        DataGenerator generator = mayGenerator.get();
                        generator.setName(name);
                        this.generators.put(name, generator);
                    }
                } catch (IOException | JsonParseException | IllegalStateException e) {
                    DataGenerator.LOGGER.error("Failed to read generator from file \"%s\"".formatted(file), e);
                }
            });
        } catch (IOException e) {
            DataGenerator.LOGGER.error("Failed to list generator directory \"%s\"".formatted(parent), e);
        }
    }

    protected void generate() {
        DataGenerator.LOGGER.debug("Starting generation for pack \"%s\" with %d generators".formatted(this.getId(), this.generators.size()));
        for (DataGenerator generator : this.generators.values()) {
            DataGenerator.LOGGER.debug("Generating data for generator \"%s\"".formatted(generator.getName() != null ? generator.getName() : "<unnamed>"));
            generator.generate(this);
        }
    }

    protected void dump() {
        try {
            for (PackCacheEntry<?> entry : this.cache.entries()) {
                Optional<?> value = entry.getCache();
                if (value.isPresent()) {
                    FileUtil.write(this.getRoot().resolve(this.proxyPath(entry.path())).toFile(), value.get().toString(), true);
                }
            }
        } catch (Exception e) {
            DataGenerator.LOGGER.error("Failed to write pack data to file system", e);
        }
    }

    public String proxyPath(String path) {
        return path;
    }

    protected void writeMeta() {
        try {
            FileUtil.write(this.root.resolve("pack.mcmeta").toFile(), GSON.toJson(this.meta), true);
        } catch (IOException e) {
            DataGenerator.LOGGER.error("Failed to write pack metadata to file system", e);
        }
    }

    public <T> T cache(String relative, T content, DataGenerator owner) {
        return this.cache.cache(relative, content, owner);
    }

    public <T> Optional<T> occupy(DataGenerator querier, String path) throws ClassCastException {
        return this.cache.occupy(querier, path);
    }

    public Set<PackCacheEntry<?>> getAll(DataGenerator querier) {
        return this.cache.getAll(querier);
    }

    public Path getRoot() {
        return this.root;
    }
}
