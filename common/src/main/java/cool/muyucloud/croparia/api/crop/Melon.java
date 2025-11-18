package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.block.MelonAttach;
import cool.muyucloud.croparia.api.crop.block.MelonBlock;
import cool.muyucloud.croparia.api.crop.block.MelonStem;
import cool.muyucloud.croparia.api.crop.item.Croparia;
import cool.muyucloud.croparia.api.crop.item.MelonItem;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.Color;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.supplier.HolderSupplier;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Melon extends AbstractCrop<Block> {
    public static final MapCodec<Melon> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CodecUtil.fieldsOf(ResourceLocation.CODEC, "id", "name").forGetter(Melon::getKey),
        CodecUtil.fieldsOf(BlockMaterial.CODEC, "material", "tag").forGetter(Melon::getMaterial),
        Color.CODEC.fieldOf("color").forGetter(Melon::getColor),
        Codec.INT.fieldOf("tier").forGetter(Melon::getTier),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations").forGetter(Melon::getTranslationsOptional),
        CropDependencies.CODEC_ANY.optionalFieldOf("dependencies", CropDependencies.EMPTY).forGetter(Melon::getDependencies)
    ).apply(instance, (name, material, color, tier, translations, dependencies) ->
        new Melon(name, material, color, tier, translations.orElse(null), dependencies)
    ));

    public static final Placeholder<Melon> PLACEHOLDER = Placeholder.build(node -> node
        .then(PatternKey.literal("color"), TypeMapper.of(Melon::getColor), Color.PLACEHOLDER)
        .then(PatternKey.literal("tier"), TypeMapper.of(Melon::getTier), Placeholder.NUMBER)
        .then(PatternKey.literal("croparia"), TypeMapper.of(melon -> melon.getCroparia().getId()), Placeholder.ID)
        .then(PatternKey.literal("material"), TypeMapper.of(Melon::getMaterial), BlockMaterial.PLACEHOLDER)
        .then(PatternKey.literal("melon"), TypeMapper.of(melon -> melon.getMelon().getId()), Placeholder.ID)
        .then(PatternKey.literal("stem"), TypeMapper.of(melon -> melon.getStem().getId()), Placeholder.ID)
        .then(PatternKey.literal("attach"), TypeMapper.of(melon -> melon.getAttach().getId()), Placeholder.ID)
        .then(PatternKey.literal("seed"), TypeMapper.of(melon -> melon.getSeed().getId()), Placeholder.ID)
        .concat(AbstractCrop.PLACEHOLDER, TypeMapper.of(melon -> melon))
    );

    private final ResourceLocation id;
    private final Color color;
    private final int tier;
    private final BlockMaterial material;
    private final ImmutableMap<String, String> translations;
    private final CropDependencies dependencies;
    private transient final String defaultTranslation;
    private transient final HolderSupplier<MelonStem> stem;
    private transient final HolderSupplier<MelonAttach> attach;
    private transient final HolderSupplier<MelonSeed> seed;
    private transient final HolderSupplier<MelonBlock> melon;
    private transient final HolderSupplier<MelonItem> melonItem;
    private transient final LazySupplier<Boolean> load = LazySupplier.of(
        () -> this.getDependencies().shouldLoad() && CropariaIf.CONFIG.isCropValid(this.getKey())
    );

    public Melon(ResourceLocation id, BlockMaterial material, Color color, int tier, Map<String, String> translations, CropDependencies dependencies) {
        this.id = id;
        this.color = color;
        this.tier = tier;
        this.material = material;
        this.defaultTranslation = defaultTranslation(this.id);
        this.dependencies = dependencies.isEmpty() ? new CropDependencies(CropariaIf.MOD_ID, "melon.%s.%s".formatted(id.getNamespace(), id.getPath())) : dependencies;
        Map<String, String> builder = new HashMap<>();
        builder.put("en_us", this.getDefaultTranslation());
        if (translations != null && !translations.isEmpty()) builder.putAll(translations);
        builder.putAll(translations == null ? Collections.emptyMap() : translations);
        this.translations = ImmutableMap.copyOf(builder);
        String path = id.getPath();
        this.stem = HolderSupplier.of(() -> new MelonStem(this), CropariaIf.of("melon_stem_%s".formatted(path)), BuiltInRegistries.BLOCK);
        this.attach = HolderSupplier.of(() -> new MelonAttach(this), CropariaIf.of("melon_attach_%s".formatted(path)), BuiltInRegistries.BLOCK);
        this.seed = HolderSupplier.of(() -> new MelonSeed(this), CropariaIf.of("melon_seed_%s".formatted(path)), BuiltInRegistries.ITEM);
        this.melon = HolderSupplier.of(() -> new MelonBlock(this), CropariaIf.of("melon_%s".formatted(path)), BuiltInRegistries.BLOCK);
        this.melonItem = HolderSupplier.of(() -> new MelonItem(this), CropariaIf.of("melon_%s".formatted(path)), BuiltInRegistries.ITEM);
    }

    public int getTier() {
        return tier;
    }

    public RegistrySupplier<Croparia> getCroparia() {
        return CropariaItems.getCroparia(this.getTier());
    }

    public Color getColor() {
        return color;
    }

    public HolderSupplier<MelonStem> getStem() {
        return stem;
    }

    public HolderSupplier<MelonAttach> getAttach() {
        return attach;
    }

    public HolderSupplier<MelonSeed> getSeed() {
        return seed;
    }

    public HolderSupplier<MelonBlock> getMelon() {
        return this.melon;
    }

    public HolderSupplier<MelonItem> getMelonItem() {
        return melonItem;
    }

    public String getDefaultTranslation() {
        return defaultTranslation;
    }

    public CropDependencies getDependencies() {
        return this.dependencies;
    }

    @Override
    public @NotNull BlockMaterial getMaterial() {
        return this.material;
    }

    @Override
    public void onRegister() {
        this.getMelon().tryRegister();
        this.getMelonItem().tryRegister();
        this.getStem().tryRegister();
        this.getAttach().tryRegister();
        this.getSeed().tryRegister();
    }

    @Override
    public Collection<String> getLangs() {
        return this.getTranslations().keySet();
    }

    @Override
    public String getTranslationKey() {
        return dependencies.getChosen();
    }

    @Override
    public @Nullable String translate(String lang) {
        return this.getTranslations().get(lang);
    }

    @Override
    public Map<String, String> getTranslations() {
        return this.translations;
    }

    public Optional<Map<String, String>> getTranslationsOptional() {
        if (this.translations.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.translations);
        }
    }

    @Override
    public @NotNull ResourceLocation getKey() {
        return this.id;
    }

    @Override
    public boolean shouldLoad() {
        return load.get();
    }

    @Override
    public Placeholder<Melon> placeholder() {
        return PLACEHOLDER;
    }
}
