package cool.muyucloud.croparia.api.generator.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.util.JarJarEntry;
import cool.muyucloud.croparia.api.generator.util.PackCache;
import cool.muyucloud.croparia.api.generator.util.PackCacheEntry;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    protected final transient Set<DataGenerator> generators = new HashSet<>();

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
        if (this.canOverride()) {
            this.clear();
        }
        this.writeMeta();
        this.moveBuiltInGenerators();
        this.refreshGenerators();
        this.generate();
        this.onGenerated();
        this.dump();
        this.onDumped();
        this.cache.clear();
    }

    protected void onGenerated() {
        this.generators.forEach(generator -> generator.onGenerated(this));
    }

    protected void onDumped() {
        this.generators.forEach(generator -> generator.onDumped(this));
    }

    protected void moveBuiltInGenerators() {
        Path targetRoot = this.getRoot().resolve("generators");
        File targetRootFile = targetRoot.toFile();
        if (!targetRootFile.isDirectory() && !targetRootFile.mkdirs()) {
            DataGenerator.LOGGER.error("Failed to establish directory \"%s\"".formatted(targetRoot));
        }
        String prefix = "data-generators/%s/%s/".formatted(this.getId().getNamespace(), this.getId().getPath());
        getBuiltinGenerators(this.getId()).forEach(entry -> {
            String name = entry.getEntry().getName();
            Path targetPath = targetRoot.resolve(name.substring(prefix.length()));
            File target = targetPath.toFile();
            File parent = targetPath.getParent().toFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                DataGenerator.LOGGER.error("Failed to establish directory \"%s\"".formatted(parent));
                return;
            }
            if (!target.isFile() || Platform.isDevelopmentEnvironment()) {
                try (OutputStream stream = new FileOutputStream(target)) {
                    entry.forInputStream(input -> {
                        try {
                            input.transferTo(stream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    stream.flush();
                } catch (IOException e) {
                    DataGenerator.LOGGER.error("Failed to move built-in generator \"%s\" from %s".formatted(name, entry.getFile().getName()), e);
                }
            }
        });
    }

    protected void refreshGenerators() {
        this.generators.clear();
        File parent = this.getRoot().resolve("generators").toFile();
        try {
            FileUtil.forFilesIn(parent, file -> DataGenerator.read(file).ifPresent(this.generators::add));
        } catch (IOException | JsonParseException e) {
            DataGenerator.LOGGER.error("Failed to read generators from directory \"%s\"".formatted(parent), e);
        }
    }

    protected void generate() {
        for (DataGenerator generator : this.generators) {
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
