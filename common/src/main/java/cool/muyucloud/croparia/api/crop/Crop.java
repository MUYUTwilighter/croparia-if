package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.block.CropariaCropBlock;
import cool.muyucloud.croparia.api.crop.item.CropFruit;
import cool.muyucloud.croparia.api.crop.item.CropSeed;
import cool.muyucloud.croparia.api.crop.util.Color;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import cool.muyucloud.croparia.api.crop.util.ItemMaterial;
import cool.muyucloud.croparia.api.crop.util.TierAccess;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.HolderSupplier;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class Crop extends AbstractCrop<Item> implements TierAccess {
    public static final Set<String> PRESET_TYPES = new HashSet<>();
    public static final String ANIMAL = addType("animal");
    public static final String CROP = addType("crop");
    public static final String FOOD = addType("food");
    public static final String MONSTER = addType("monster");
    public static final String NATURE = addType("nature");
    public static final String DEFAULT_TYPE = CROP;

    public static String addType(String type) {
        PRESET_TYPES.add(type);
        return type;
    }

    public static final MapCodec<Crop> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CodecUtil.fieldsOf(ResourceLocation.CODEC, "id", "name").forGetter(Crop::getKey),
        CodecUtil.fieldsOf(ItemMaterial.CODEC, "material", "tag").forGetter(Crop::getMaterial),
        Color.CODEC.fieldOf("color").forGetter(Crop::getColor),
        Codec.INT.fieldOf("tier").forGetter(Crop::getTier),
        Codec.STRING.optionalFieldOf("type", "crop").forGetter(Crop::getType),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations").forGetter(Crop::getTranslationsOptional),
        CropDependencies.CODEC_ANY.optionalFieldOf("dependencies", CropDependencies.EMPTY).forGetter(Crop::getDependencies)
    ).apply(instance, (name, material, color, tier, type, translations, dependencies) ->
        new Crop(name, material, color, tier, type, translations.orElse(null), dependencies)
    ));

    public static final Placeholder<Crop> PLACEHOLDER = Placeholder.build(node -> node
        .then(PatternKey.literal("color"), TypeMapper.of(Crop::getColor), Color.PLACEHOLDER)
        .then(PatternKey.literal("type"), TypeMapper.of(Crop::getType), Placeholder.STRING)
        .then(PatternKey.literal("material"), TypeMapper.of(Crop::getMaterial), ItemMaterial.PLACEHOLDER)
        .then(PatternKey.literal("tier"), TypeMapper.of(Crop::getTier), Placeholder.NUMBER)
        .then(PatternKey.literal("seed"), TypeMapper.of(Crop::getSeedId), Placeholder.ID)
        .then(PatternKey.literal("fruit"), TypeMapper.of(Crop::getFruitId), Placeholder.ID)
        .then(PatternKey.literal("crop_block"), TypeMapper.of(Crop::getBlockId), Placeholder.ID)
        .then(PatternKey.literal("croparia"), TypeMapper.of(crop -> CropariaItems.getCroparia(crop.getTier()).getId()), Placeholder.ID)
        .concat(AbstractCrop.PLACEHOLDER, TypeMapper.of(crop -> crop))
    );

    public static String defaultTranslationKey(ResourceLocation id) {
        return "crop.%s.%s".formatted(id.getNamespace(), id.getPath());
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final ItemMaterial material;
    @NotNull
    private final Color color;
    private final int tier;
    @NotNull
    private final String type;
    @NotNull
    private final ImmutableMap<String, String> translations;
    @NotNull
    private final CropDependencies dependencies;
    @NotNull
    private final transient HolderSupplier<CropariaCropBlock> block;
    @NotNull
    private final transient HolderSupplier<CropSeed> seed;
    @NotNull
    private final transient HolderSupplier<CropFruit> fruit;
    private transient final String defaultTranslation;
    private transient final String defaultTranslationKey;
    private transient final LazySupplier<Boolean> load = LazySupplier.of(
        () -> this.getDependencies().shouldLoad() && CropariaIf.CONFIG.isCropValid(this.getKey())
    );

    public Crop(
        @NotNull ResourceLocation id, @NotNull ItemMaterial material, @NotNull Color color, int tier, @Nullable String type,
        @Nullable Map<String, String> translations, @NotNull CropDependencies dependencies
    ) {
        this.id = id;
        this.material = material;
        this.color = color;
        this.tier = tier;
        this.defaultTranslationKey = defaultTranslationKey(id);
        this.dependencies = dependencies.isEmpty() ? new CropDependencies(CropariaIf.MOD_ID, this.getDefaultTranslationKey()) : dependencies;
        this.defaultTranslation = defaultTranslation(id);
        Map<String, String> builder = new HashMap<>();
        builder.put("en_us", this.getDefaultTranslation());
        builder.putAll(translations == null ? Collections.emptyMap() : translations);
        this.translations = ImmutableMap.copyOf(builder);
        this.type = type == null ? DEFAULT_TYPE : type;
        this.block = HolderSupplier.of(() -> new CropariaCropBlock(this), CifUtil.formatId("block_crop_%s", this.getKey()), Registries.BLOCK);
        this.seed = HolderSupplier.of(() -> new CropSeed(this), CifUtil.formatId("crop_seed_%s", this.getKey()), Registries.ITEM);
        this.fruit = HolderSupplier.of(() -> new CropFruit(this), CifUtil.formatId("fruit_%s", this.getKey()), Registries.ITEM);
    }

    @Override
    public @NotNull ResourceLocation getKey() {
        return this.id;
    }

    @Override
    public @NotNull ItemMaterial getMaterial() {
        return material;
    }

    public DataComponentPatch getPatch() {
        return this.getMaterial().getComponents();
    }

    public @NotNull Color getColor() {
        return color;
    }

    public int getColorInt() {
        return this.getColor().getValue();
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public Collection<String> getLangs() {
        return this.getTranslations().keySet();
    }

    public @NotNull String getTranslationKey() {
        return dependencies.getChosen();
    }

    public Optional<String> getTranslationKeyOptional() {
        if (this.getTranslationKey().equals(defaultTranslationKey)) {
            return Optional.empty();
        } else {
            return Optional.of(this.getTranslationKey());
        }
    }

    @Override
    public @NotNull ImmutableMap<String, String> getTranslations() {
        return translations;
    }

    @Override
    @Nullable
    public String translate(String lang) {
        return this.getTranslations().get(lang);
    }

    public Optional<Map<String, String>> getTranslationsOptional() {
        Map<String, String> translations = this.getTranslations();
        if (translations.size() == 1 && translations.containsValue(this.getDefaultTranslation())) {
            return Optional.empty();
        } else {
            return Optional.of(translations);
        }
    }

    public @NotNull String getType() {
        return type;
    }

    public Optional<String> getTypeOptional() {
        return this.getType().equals(DEFAULT_TYPE) ? Optional.empty() : Optional.of(this.getType());
    }

    public @NotNull CropDependencies getDependencies() {
        return this.dependencies;
    }

    public @NotNull ResourceLocation getBlockId() {
        return block.getId();
    }

    public Optional<CropariaCropBlock> getCropBlock() {
        return block.toOptional();
    }

    public @NotNull ResourceLocation getSeedId() {
        return seed.getId();
    }

    public Optional<CropSeed> getCropSeed() {
        return seed.toOptional();
    }

    public @NotNull ResourceLocation getFruitId() {
        return fruit.getId();
    }

    public Optional<CropFruit> getCropFruit() {
        return fruit.toOptional();
    }

    public String getDefaultTranslation() {
        return defaultTranslation;
    }

    public String getDefaultTranslationKey() {
        return defaultTranslationKey;
    }

    @Override
    public Placeholder<? extends Crop> placeholder() {
        return PLACEHOLDER;
    }

    @Override
    public boolean shouldLoad() {
        return load.get();
    }

    @Override
    public void onRegister() {
        this.block.tryRegister();
        this.seed.tryRegister();
        this.fruit.tryRegister();
    }
}
