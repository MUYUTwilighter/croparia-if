package cool.muyucloud.croparia.api.crop.util;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.util.TagUtil;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Material {
    public static final Codec<Material> CODEC_STR = Codec.STRING.xmap(Material::new, Material::getName);
    public static final MapCodec<Material> CODEC_COMP = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Material::getName),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(Material::getComponents),
            Codec.INT.optionalFieldOf("count", 2).forGetter(Material::getCount)
        ).apply(instance, Material::new)
    );
    public static final MultiCodec<Material> CODEC = CodecUtil.of(
        CodecUtil.of(CODEC_COMP.codec(), toDecode -> toDecode.components.equals(DataComponentPatch.EMPTY)
            ? TestedCodec.fail(() -> "No components, proceed to string codec") : TestedCodec.success()), CODEC_STR
    );
    public static final Placeholder<Material> PLACEHOLDER = Placeholder.build(node -> node
        .self(TypeMapper.identity(), Material.CODEC)
        .then(PatternKey.literal("type"), TypeMapper.of(material -> material.isTag() ? "tag" : "item"), Placeholder.STRING)
        .then(PatternKey.literal("name"), TypeMapper.of(Material::getName), Placeholder.STRING)
        .then(PatternKey.literal("count"), TypeMapper.of(Material::getCount), Placeholder.NUMBER)
        .then(PatternKey.literal("components"), TypeMapper.of(Material::getComponents), Placeholder.DATA_COMPONENTS)
        .then(PatternKey.literal("id"), TypeMapper.of(Material::getId), Placeholder.ID));

    private final boolean tag;
    @NotNull
    private final ResourceLocation id;
    private final int count;
    @NotNull
    private final DataComponentPatch components;
    private transient final OnLoadSupplier<List<Item>> items = OnLoadSupplier.of(() -> {
        List<Item> tmp = new ArrayList<>();
        if (this.isTag()) {
            for (Holder<Item> holder : TagUtil.forEntries(Registries.ITEM, this.getId())) {
                tmp.add(holder.value());
            }
        } else {
            tmp.add(BuiltInRegistries.ITEM.getValue(this.getId()));
        }
        if (tmp.isEmpty()) tmp.add(Items.AIR);
        return ImmutableList.copyOf(tmp);
    });

    public Material(ItemStack stack) {
        this(String.valueOf(stack.getItem().arch$registryName()), stack.getComponentsPatch(), stack.getCount());
    }

    public Material(@NotNull String name) {
        this(name, DataComponentPatch.EMPTY, 2);
    }

    public Material(@NotNull String name, @NotNull DataComponentPatch components, int count) {
        if (name.startsWith("#")) {
            this.tag = true;
            this.id = ResourceLocation.parse(name.substring(1));
            this.count = count;
        } else {
            this.tag = false;
            this.id = ResourceLocation.parse(name);
            this.count = count;
        }
        this.components = components;
    }

    public boolean isTag() {
        return tag;
    }

    public @NotNull ResourceLocation getId() {
        return id;
    }

    public @NotNull DataComponentPatch getComponents() {
        return components;
    }

    public String getName() {
        return this.isTag() ? "#" + this.getId() : this.getId().toString();
    }

    public @NotNull Item getItem() {
        return this.getItems().getFirst();
    }

    public List<Item> getItems() {
        return this.items.get();
    }

    public ItemStack getStack() {
        ItemStack stack = new ItemStack(this.getItem());
        stack.applyComponents(this.getComponents());
        stack.setCount(this.getCount());
        return stack;
    }

    public int getCount() {
        return count;
    }
}
