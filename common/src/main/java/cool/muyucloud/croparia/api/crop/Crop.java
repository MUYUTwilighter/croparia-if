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
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.crop.util.TierAccess;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.HolderSupplier;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Crop extends AbstractCrop implements TierAccess {
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
        CodecUtil.fieldsOf(Material.CODEC, "material", "tag").forGetter(Crop::getMaterial),
        Color.CODEC.fieldOf("color").forGetter(Crop::getColor),
        Codec.INT.fieldOf("tier").forGetter(Crop::getTier),
        Codec.STRING.optionalFieldOf("type", "crop").forGetter(Crop::getType),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("translations").forGetter(Crop::getTranslationsOptional),
        CropDependencies.CODEC_ANY.optionalFieldOf("dependencies").forGetter(Crop::getDependenciesOptional)
    ).apply(instance, (name, material, color, tier, type, translations, dependencies) ->
        new Crop(name, material, color, tier, type, translations.orElse(null), dependencies.orElse(null))
    ));

    public static final Placeholder<Crop> PLACEHOLDER = Placeholder.build(node -> node
        .then(Pattern.compile("^color$"), Crop::getColor, Color.PLACEHOLDER)
        .then(Pattern.compile("^type$"), Crop::getType, Placeholder.STRING)
        .then(Pattern.compile("^tier$"), Crop::getTier, Placeholder.NUMBER)
        .then(Pattern.compile("^seed$"), Crop::getSeedId, Placeholder.ID)
        .then(Pattern.compile("^fruit$"), Crop::getFruitId, Placeholder.ID)
        .then(Pattern.compile("^crop_block$"), Crop::getBlockId, Placeholder.ID)
        .then(Pattern.compile("^croparia$"), crop -> CropariaItems.getCroparia(crop.getTier()).getId(), Placeholder.ID)
        .concat(AbstractCrop.PLACEHOLDER, crop -> crop)
    );

    public static String defaultTranslation(ResourceLocation id) {
        String name = id.getPath();
        name = name.replaceAll("_", " ").trim();
        StringBuilder builder = new StringBuilder();
        for (String token : name.split(" ")) {
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1)).append(" ");
        }
        return builder.toString().trim();
    }

    public static String defaultTranslationKey(ResourceLocation id) {
        return "crop.%s.%s".formatted(id.getNamespace(), id.getPath());
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final Material material;
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
        @NotNull ResourceLocation id, @NotNull Material material, @NotNull Color color, int tier, @Nullable String type,
        @Nullable Map<String, String> translations, @Nullable CropDependencies dependencies
    ) {
        this.id = id;
        this.material = material;
        this.color = color;
        this.tier = tier;
        this.defaultTranslationKey = defaultTranslationKey(id);
        this.dependencies = dependencies == null || dependencies.isEmpty() ? new CropDependencies(CropariaIf.MOD_ID, this.getDefaultTranslationKey()) : dependencies;
        this.defaultTranslation = defaultTranslation(id);
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        this.translations = builder.put("en_us", this.getDefaultTranslation()).putAll(translations == null ? Collections.emptyMap() : translations).build();
        this.type = type == null ? DEFAULT_TYPE : type;
        this.block = HolderSupplier.of(() -> new CropariaCropBlock(this), CifUtil.formatId("block_crop_%s", this.getKey()), Registries.BLOCK);
        this.seed = HolderSupplier.of(() -> new CropSeed(this), CifUtil.formatId("crop_seed_%s", this.getKey()), Registries.ITEM);
        this.fruit = HolderSupplier.of(() -> new CropFruit(this), CifUtil.formatId("fruit_%s", this.getKey()), Registries.ITEM);
        this.results = this.results.map(stacks -> stacks.stream().filter(stack -> CropariaIf.CONFIG.isModValid(
            Objects.requireNonNull(stack.getItem().arch$registryName()).getNamespace()
        )).toList());
    }

    @Override
    public @NotNull ResourceLocation getKey() {
        return this.id;
    }

    @Override
    public @NotNull Material getMaterial() {
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

    public String getColorDec() {
        return this.getColor().toDecString();
    }

    public String getColorHex() {
        return this.getColor().toHexString();
    }

    public String getColorForm() {
        return this.getColor().toString();
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
        if (translations.size() == 1 && translations.containsValue(this.getDefaultTranslationKey())) {
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

    public Optional<CropDependencies> getDependenciesOptional() {
        return this.getDependencies().size() <= 1 && this.getDependencies().getKey(CropariaIf.MOD_ID) != null ? Optional.empty() :
            Optional.of(this.getDependencies());
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
