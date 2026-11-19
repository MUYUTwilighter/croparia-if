package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.asset.Color;
import cool.muyucloud.croparia.api.crop.asset.ColorAccess;
import cool.muyucloud.croparia.api.crop.block.CropariaCrop;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import cool.muyucloud.croparia.api.crop.util.ItemMaterial;
import cool.muyucloud.croparia.api.crop.util.TierAccess;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class Crop extends AbstractCrop<Item> implements TierAccess, ColorAccess {
    public static final String ANIMAL = "animal";
    public static final String CROP = "crop";
    public static final String FOOD = "food";
    public static final String MONSTER = "monster";
    public static final String NATURE = "nature";
    public static final String DEFAULT_TYPE = CROP;
    public static final Set<String> PRESET_TYPES = new HashSet<>(List.of(ANIMAL, CROP, FOOD, MONSTER, NATURE));

    public static String addType(String type) {
        PRESET_TYPES.add(type);
        return type;
    }

    public static final MapCodec<Crop> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CodecUtil.fieldsOf(Identifier.CODEC, "id", "name").forGetter(Crop::getKey),
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
        .then(PatternKey.literal("croparia"), TypeMapper.of(crop -> CropariaItems.getCroparia(crop.getTier()).getId()), Placeholder.ID)
        .concat(AbstractCrop.PLACEHOLDER, TypeMapper.of(crop -> crop))
    );

    public static String defaultTranslationKey(Identifier id) {
        return "crop.%s.%s".formatted(id.getNamespace(), id.getPath());
    }

    @NotNull
    private final Identifier id;
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
    private transient final String defaultTranslation;
    private transient final String defaultTranslationKey;
    private transient final LazySupplier<Boolean> load = LazySupplier.of(
        () -> this.getDependencies().shouldLoad() && CropariaIf.CONFIG.isCropValid(this.getKey())
    );

    public Crop(
        @NotNull Identifier id, @NotNull ItemMaterial material, @NotNull Color color, int tier, @Nullable String type,
        @Nullable Map<String, String> translations, @NotNull CropDependencies dependencies
    ) {
        this.id = CropariaIf.of(id.getPath());
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
    }

    @Override
    public @NotNull Identifier getKey() {
        return this.id;
    }

    @Override
    public @NotNull ItemMaterial getMaterial() {
        return material;
    }

    public DataComponentPatch getPatch() {
        return this.getMaterial().getComponents();
    }

    @Override
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

    public Optional<Map<String, String>> getTranslationsOptional() {
        Map<String, String> translations = this.getTranslations();
        if (translations.size() == 1 && translations.containsValue(this.getDefaultTranslation())) {
            return Optional.empty();
        } else {
            return Optional.of(translations);
        }
    }

    /**
     * This does not invalidate whether the return value is possible values for {@link CropariaCrop#TYPE}
     */
    public @NotNull String getType() {
        return type;
    }

    public Optional<String> getTypeOptional() {
        return this.getType().equals(DEFAULT_TYPE) ? Optional.empty() : Optional.of(this.getType());
    }

    public @NotNull CropDependencies getDependencies() {
        return this.dependencies;
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
    public ItemStack getFruit() {
        ItemStack stack = CropariaItems.CROP_FRUIT.get().getDefaultInstance();
        stack.set(CropariaComponents.CROP_ID.get(), this.getKey());
        return stack;
    }

    public ItemStack getSeed() {
        return CropariaItems.CROP_SEED.get().getCropItem(this.getKey());
    }
}
