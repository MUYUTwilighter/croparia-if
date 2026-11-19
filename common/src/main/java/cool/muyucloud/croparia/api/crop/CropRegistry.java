package cool.muyucloud.croparia.api.crop;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.SemiSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

// TODO: sync and cache crop data, add data reload listener
public class CropRegistry<C extends AbstractCrop<?>> implements DgRegistry<C> {
    public static final Codec<CropRegistry<?>> CODEC_BY_NAME = DgRegistry.CODEC_BY_NAME.xmap(dgReg -> {
        if (dgReg instanceof CropRegistry) {
            return (CropRegistry<?>) dgReg;
        } else {
            throw new ClassCastException("Trying to decode a DgRegistry that is not a CropRegistry");
        }
    }, cropReg -> cropReg);

    private static final Gson GSON = new Gson();

    private final Path path;
    private final Codec<C> codec;
    private final List<Supplier<? extends Collection<C>>> external = new ArrayList<>();
    private final Map<Identifier, C> crops = new HashMap<>();
    private final SemiSupplier<List<CompoundTag>> nbtCache = SemiSupplier.of(() -> {
        ArrayList<CompoundTag> tags = new ArrayList<>();
        crops.forEach((id, crop) -> getCropCodec().encodeStart(NbtOps.INSTANCE, crop).ifError(
            e -> CropariaIf.LOGGER.error("Failed to decode crop {}: {}", id, e.message())
        ).ifSuccess(tag -> tags.add((CompoundTag) tag)));
        tags.trimToSize();
        return tags;
    });

    public CropRegistry(Path path, Codec<C> codec) {
        this.path = path;
        this.codec = codec;
    }

    public CropRegistry<C> withExternal(Supplier<? extends Collection<C>> external) {
        this.external.add(external);
        return this;
    }


    @Override
    public Optional<C> getById(Identifier id) {
        return Optional.ofNullable(this.crops.get(id));
    }

    public Path getPath() {
        return path;
    }

    public Codec<C> getCropCodec() {
        return codec;
    }

    public List<CompoundTag> getNbt() {
        return nbtCache.get();
    }

    /**
     * Remote crop syncing behavior.
     *
     * @param crops crop definitions to load
     */
    public void syncCrops(Collection<C> crops) {
        this.crops.clear();
        crops.forEach(crop -> this.crops.put(crop.getKey(), crop));
    }

    /**
     * Reloads local definitions using the external definitions supplied during initialization.
     */
    public void loadCrops() {
        this.crops.clear();
        this.external.forEach(provider -> provider.get().forEach(this::register));
        this.readCrops();
        nbtCache.refresh();
    }

    public void readCrops() {
        File parent = this.getPath().toFile();
        try {
            FileUtil.ensureDirectory(parent);
            FileUtil.forFilesIn(parent, this::readCrop);
        } catch (IOException e) {
            CropariaIf.LOGGER.error("Failed to read crops", e);
        }
    }

    protected void readCrop(File file) {
        if (!file.getName().endsWith(".json")) return;
        try {
            CodecUtil.readJson(file, this.getCropCodec()).ifSuccess(this::register);
        } catch (IOException | RuntimeException e) {
            CropariaIf.LOGGER.error("Failed to read crop from file \"%s\"".formatted(file), e);
        }
    }

    public void register(C crop) {
        if (crop.shouldLoad()) {
            crops.put(crop.getKey(), crop);
        } else {
            crops.remove(crop.getKey());
        }
    }

    public void dumpCrops() {
        this.crops.values().forEach(this::dumpCrop);
    }

    public Path dumpCrop(@NotNull C crop) {
        Path cropPath = this.getPath().resolve(crop.getKey().getPath() + ".json");
        StringWriter result = new StringWriter();
        CodecUtil.encodeJson(crop, this.getCropCodec()).mapOrElse(json -> {
            try (JsonWriter writer = new JsonWriter(result)) {
                writer.setIndent("  ");
                GSON.toJson(json, writer);
                FileUtil.write(cropPath.toFile(), result.toString(), true);
            } catch (IOException e) {
                CropariaIf.LOGGER.error("Failed to dump crop \"{}\"", crop.getKey(), e);
            }
            return null;
        }, e -> {
            CropariaIf.LOGGER.error("Failed to dump crop \"{}\": {}", crop.getKey(), e.message());
            return null;
        });
        return cropPath;
    }

    public boolean exists(Identifier name) {
        return crops.containsKey(name);
    }

    @NotNull
    @Override
    public Iterator<C> iterator() {
        return this.crops.values().iterator();
    }

    public int size() {
        return this.crops.size();
    }
}
