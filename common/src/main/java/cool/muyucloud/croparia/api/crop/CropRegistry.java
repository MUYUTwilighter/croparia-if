package cool.muyucloud.croparia.api.crop;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.util.FileUtil;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CropRegistry<C extends AbstractCrop<?>> implements DgRegistry<C> {
    private static final Gson GSON = new Gson();

    private final Path path;
    private final Codec<C> codec;
    private final Map<ResourceLocation, C> all = new HashMap<>();
    private final Map<ResourceLocation, C> loaded = new HashMap<>();

    public CropRegistry(Path path, Codec<C> codec) {
        this.path = path;
        this.codec = codec;
    }

    public Path getPath() {
        return path;
    }

    public Codec<C> getCodec() {
        return codec;
    }

    public void forLoaded(Consumer<C> consumer) {
        this.loaded.values().forEach(consumer);
    }

    public void register(C crop) {
        this.all.put(crop.getKey(), crop);
        if (crop.shouldLoad()) {
            if (!loaded.containsKey(crop.getKey())) {
                crop.onRegister();
            }
            loaded.put(crop.getKey(), crop);
        } else {
            loaded.remove(crop.getKey());
        }
    }

    public void readCrops() {
        File file = this.getPath().toFile();
        if (!file.isDirectory() && !file.mkdirs()) {
            throw new IllegalStateException("Failed to establish directory \"%s\"".formatted(file));
        }
        this.readCrops(this.getPath());
    }

    protected void readCrops(Path parent) {
        try (Stream<Path> paths = Files.list(parent)) {
            paths.forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    readCrop(file);
                } else {
                    readCrops(path);
                }
            });
        }  catch (IOException e) {
            CropariaIf.LOGGER.error("Failed to read crops", e);
        }
    }

    protected void readCrop(File file) {
        if (!file.getName().endsWith(".json")) return;
        try {
            CodecUtil.readJson(file, this.getCodec()).ifSuccess(this::register);
        } catch (IOException e) {
            CropariaIf.LOGGER.error("Failed to read crop from file \"%s\"".formatted(file), e);
        }
    }

    public void dumpCrops() {
        all.values().forEach(this::dumpCrop);
    }

    public Path dumpCrop(@NotNull C crop) {
        Path cropPath = this.getPath().resolve(crop.getKey().toString().replace(":", "/") + ".json");
        StringWriter result = new StringWriter();
        CodecUtil.encodeJson(crop, this.getCodec()).mapOrElse(json -> {
            try (JsonWriter writer = new JsonWriter(result)) {
                writer.setIndent("  ");
                GSON.toJson(json, writer);
                FileUtil.write(cropPath.toFile(), result.toString(), true);
            } catch (IOException e) {
                CropariaIf.LOGGER.error("Failed to dump crop \"%s\"".formatted(crop.getKey()), e);
            }
            return null;
        }, e -> {
            CropariaIf.LOGGER.error("Failed to dump crop \"%s\": %s".formatted(crop.getKey(), e.message()));
            return null;
        });
        return cropPath;
    }

    public boolean exists(ResourceLocation name) {
        return all.containsKey(name);
    }

    @NotNull
    @Override
    public Iterator<C> iterator() {
        return this.all.values().iterator();
    }

    @Override
    public Optional<C> forName(ResourceLocation id) {
        return Optional.ofNullable(this.all.get(id));
    }

    public int size() {
        return this.all.size();
    }
}
