package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.asset.Color;
import cool.muyucloud.croparia.api.crop.asset.ColorAccess;
import cool.muyucloud.croparia.api.crop.item.Croparia;
import cool.muyucloud.croparia.api.crop.util.BlockMaterial;
import cool.muyucloud.croparia.api.crop.util.CropDependencies;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Melon extends AbstractCrop<Block> implements ColorAccess {
    public static final MapCodec<Melon> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CodecUtil.fieldsOf(Identifier.CODEC, "id", "name").forGetter(Melon::getKey),
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
        .concat(AbstractCrop.PLACEHOLDER, TypeMapper.of(melon -> melon))
    );

    private final Identifier id;
    private final Color color;
    private final int tier;
    private final BlockMaterial material;
    private final ImmutableMap<String, String> translations;
    private final CropDependencies dependencies;
    private transient final String defaultTranslation;
    private transient final LazySupplier<Boolean> load = LazySupplier.of(
        () -> this.getDependencies().shouldLoad() && CropariaIf.CONFIG.isCropValid(this.getKey())
    );

    public Melon(Identifier id, BlockMaterial material, Color color, int tier, Map<String, String> translations, CropDependencies dependencies) {
        this.id = CropariaIf.of(id.getPath());
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
    }

    @Override
    public ItemStack getFruit() {
        ItemStack stack = CropariaItems.MELON_ITEM.get().getDefaultInstance();
        stack.set(CropariaComponents.CROP_ID.get(), this.getKey());
        return stack;
    }

    public ItemStack getSeed() {
        return CropariaItems.MELON_SEED.get().getCropItem(this.getKey());
    }

    public int getTier() {
        return tier;
    }

    public RegistrySupplier<Croparia> getCroparia() {
        return CropariaItems.getCroparia(this.getTier());
    }

    @Override
    public Color getColor() {
        return color;
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
    public Collection<String> getLangs() {
        return this.getTranslations().keySet();
    }

    @Override
    public String getTranslationKey() {
        return dependencies.getChosen();
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
    public @NotNull Identifier getKey() {
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

    @Override
    public void onRegister() {
        // TODO: Add CreativeTab Content
    }
}
