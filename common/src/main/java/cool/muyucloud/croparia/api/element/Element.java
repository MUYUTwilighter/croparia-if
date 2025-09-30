package cool.muyucloud.croparia.api.element;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.util.Color;
import cool.muyucloud.croparia.api.element.block.ElementalLiquidBlock;
import cool.muyucloud.croparia.api.element.fluid.ElementalFlowing;
import cool.muyucloud.croparia.api.element.fluid.ElementalSource;
import cool.muyucloud.croparia.api.element.item.ElementalBucket;
import cool.muyucloud.croparia.api.element.item.ElementalGem;
import cool.muyucloud.croparia.api.element.item.ElementalPotion;
import cool.muyucloud.croparia.api.generator.util.DgEntry;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.CropariaFluids;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.registry.Tabs;
import cool.muyucloud.croparia.util.CifUtil;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public enum Element implements DgEntry, StringRepresentable, Comparable<Element> {
    EMPTY,
    AIR("air", Color.of(6453378), attr -> {
    }),
    EARTH("earth", Color.of(11884313), attr -> {
    }),
    ELEMENTAL("elemental", Color.of(6238065), attr -> {
    }),
    FIRE("fire", Color.of(11010055), attr -> {
    }),
    WATER("water", Color.of(750238), attr -> {
    });

    public static Element parse(String name) {
        return valueOf(name.toUpperCase());
    }

    public static final Placeholder<Element> PLACEHOLDER = Placeholder.build(builder -> builder
        .then(PatternKey.literal("name"), TypeMapper.of(Element::getSerializedName), Placeholder.STRING)
        .then(PatternKey.literal("color"), TypeMapper.of(Element::getColor), Color.PLACEHOLDER)
        .then(PatternKey.literal("fluid_source"), TypeMapper.of(element -> element.getFluidSource().getId()), Placeholder.ID)
        .then(PatternKey.literal("fluid_flowing"),TypeMapper.of(element -> element.getFluidFlowing().getId()), Placeholder.ID)
        .then(PatternKey.literal("liquid_block"),TypeMapper.of(element -> element.getFluidBlock().getId()), Placeholder.ID)
        .then(PatternKey.literal("bucket"), TypeMapper.of(element -> element.getBucket().getId()), Placeholder.ID)
        .then(PatternKey.literal("potion"), TypeMapper.of(element -> element.getPotion().getId()), Placeholder.ID)
        .then(PatternKey.literal("gem"), TypeMapper.of(element -> element.getGem().getId()), Placeholder.ID)
        .overwrite(DgEntry.PLACEHOLDER, TypeMapper.of(element -> element))
    );

    private final ResourceLocation id;
    private final Color color;
    private final RegistrySupplier<ElementalSource> fluidSource;
    private final RegistrySupplier<ElementalFlowing> fluidFlowing;
    private final RegistrySupplier<ElementalLiquidBlock> fluidBlock;
    private final RegistrySupplier<ElementalBucket> bucket;
    private final RegistrySupplier<ElementalPotion> potion;
    private final RegistrySupplier<ElementalGem> gem;

    Element() {
        this.id = CropariaIf.of("empty");
        this.color = new Color(-1);
        this.fluidSource = null;
        this.fluidFlowing = null;
        this.fluidBlock = null;
        this.bucket = null;
        this.potion = null;
        this.gem = null;
    }

    @SuppressWarnings("UnstableApiUsage")
    Element(String name, Color color, Consumer<SimpleArchitecturyFluidAttributes> appendix) {
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Element name must only contain lowercase letters, numbers, underscores");
        }
        this.id = CropariaIf.of(name);
        this.color = color;
        SimpleArchitecturyFluidAttributes attr = SimpleArchitecturyFluidAttributes.of(
            () -> Element.this.getFluidFlowing().get(),
            () -> Element.this.getFluidSource().get()
        ).block(() -> Optional.ofNullable(Element.this.getFluidBlock().get())).bucketItem(
            () -> Optional.ofNullable(Element.this.getBucket().get())
        ).sourceTexture(parseId("block/%s_still")).flowingTexture(parseId("block/%s_flow"));
        appendix.accept(attr);
        this.fluidSource = CropariaFluids.registerFluid(parseId("fluid_%s"), () -> new ElementalSource(this, attr));
        this.fluidFlowing = CropariaFluids.registerFluid(parseId("fluid_%s_flow"), () -> new ElementalFlowing(this, attr));
        this.fluidBlock = CropariaBlocks.registerBlock(parseId("fluid_%s"), properties -> new ElementalLiquidBlock(this, BlockBehaviour.Properties
            .of().setId(ResourceKey.create(Registries.BLOCK, parseId("fluid_%s")))
            .lightLevel(state -> 8).noCollission().strength(100.0F).noLootTable()
        ));
        this.bucket = CropariaItems.registerItem(parseId("bucket_%s"), properties -> new ElementalBucket(
            this, this.getFluidSource(),
            properties.stacksTo(1).arch$tab(Tabs.MAIN).craftRemainder(Items.GLASS_BOTTLE)
        ));
        this.potion = CropariaItems.registerItem(parseId("potion_%s"), properties -> new ElementalPotion(
            this, properties.arch$tab(Tabs.MAIN).craftRemainder(Items.GLASS_BOTTLE)
        ));
        this.gem = CropariaItems.registerItem(parseId("gem_%s"), properties -> new ElementalGem(
            this, properties.arch$tab(Tabs.MAIN)
        ));
    }

    public ResourceLocation parseId(String pattern) {
        return CifUtil.formatId(pattern, this.getKey());
    }

    public Color getColor() {
        return color;
    }

    public RegistrySupplier<ElementalFlowing> getFluidFlowing() {
        return fluidFlowing;
    }

    public RegistrySupplier<ElementalSource> getFluidSource() {
        return fluidSource;
    }

    public RegistrySupplier<ElementalLiquidBlock> getFluidBlock() {
        return fluidBlock;
    }

    public RegistrySupplier<ElementalBucket> getBucket() {
        return bucket;
    }

    public RegistrySupplier<ElementalPotion> getPotion() {
        return potion;
    }

    public RegistrySupplier<ElementalGem> getGem() {
        return gem;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.getKey().getPath();
    }

    @Override
    public @NotNull ResourceLocation getKey() {
        return this.id;
    }

    @Override
    public Placeholder<Element> placeholder() {
        return PLACEHOLDER;
    }

    @Override
    public boolean shouldLoad() {
        return this != EMPTY;
    }
}
