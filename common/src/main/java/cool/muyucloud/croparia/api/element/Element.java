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
import cool.muyucloud.croparia.registry.Tabs;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.HolderSupplier;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
    private final HolderSupplier<ElementalSource> fluidSource;
    private final HolderSupplier<ElementalFlowing> fluidFlowing;
    private final HolderSupplier<ElementalLiquidBlock> fluidBlock;
    private final HolderSupplier<ElementalBucket> bucket;
    private final HolderSupplier<ElementalPotion> potion;
    private final HolderSupplier<ElementalGem> gem;

    Element() {
        this.id = CropariaIf.of("empty");
        this.color = new Color(-1);
        this.fluidSource = HolderSupplier.of(() -> null, CropariaIf.of("fluid_empty"), Registries.FLUID);
        this.fluidFlowing = HolderSupplier.of(() -> null, CropariaIf.of("fluid_empty_flow"), Registries.FLUID);
        this.fluidBlock = HolderSupplier.of(() -> null, CropariaIf.of("fluid_empty"), Registries.BLOCK);
        this.bucket = HolderSupplier.of(() -> null, CropariaIf.of("bucket_empty"), Registries.ITEM);
        this.potion = HolderSupplier.of(() -> null, CropariaIf.of("potion_empty"), Registries.ITEM);
        this.gem = HolderSupplier.of(() -> null, CropariaIf.of("gem_empty"), Registries.ITEM);
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
        this.fluidSource = HolderSupplier.of(() -> new ElementalSource(this, attr), parseId("fluid_%s"), Registries.FLUID);
        this.fluidFlowing = HolderSupplier.of(() -> new ElementalFlowing(this, attr), parseId("fluid_%s_flow"), Registries.FLUID);
        this.fluidBlock = HolderSupplier.of(() -> new ElementalLiquidBlock(this, BlockBehaviour.Properties
            .ofFullCopy(Blocks.WATER).lightLevel(state -> 8)
            .setId(ResourceKey.create(Registries.BLOCK, parseId("fluid_%s")))
        ), parseId("fluid_%s"), Registries.BLOCK);
        this.bucket = HolderSupplier.of(() -> new ElementalBucket(
            this, this.getFluidSource(),
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, parseId("bucket_%s")))
                .stacksTo(1).arch$tab(Tabs.MAIN).craftRemainder(Items.GLASS_BOTTLE)
        ), parseId("bucket_%s"), Registries.ITEM);
        this.potion = HolderSupplier.of(() -> new ElementalPotion(
            this, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, parseId("potion_%s")))
            .arch$tab(Tabs.MAIN).craftRemainder(Items.GLASS_BOTTLE)
        ), parseId("potion_%s"), Registries.ITEM);
        this.gem = HolderSupplier.of(() -> new ElementalGem(
            this, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, parseId("gem_%s")))
            .arch$tab(Tabs.MAIN).craftRemainder(Items.GLASS_BOTTLE)
        ), parseId("gem_%s"), Registries.ITEM);
        this.getFluidSource().tryRegister();
        this.getFluidFlowing().tryRegister();
        this.getFluidBlock().tryRegister();
        this.getBucket().tryRegister();
        this.getPotion().tryRegister();
        this.getGem().tryRegister();
    }

    public ResourceLocation parseId(String pattern) {
        return CifUtil.formatId(pattern, this.getKey());
    }

    public Color getColor() {
        return color;
    }

    public HolderSupplier<ElementalFlowing> getFluidFlowing() {
        return fluidFlowing;
    }

    public HolderSupplier<ElementalSource> getFluidSource() {
        return fluidSource;
    }

    public HolderSupplier<ElementalLiquidBlock> getFluidBlock() {
        return fluidBlock;
    }

    public HolderSupplier<ElementalBucket> getBucket() {
        return bucket;
    }

    public HolderSupplier<ElementalPotion> getPotion() {
        return potion;
    }

    public HolderSupplier<ElementalGem> getGem() {
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
