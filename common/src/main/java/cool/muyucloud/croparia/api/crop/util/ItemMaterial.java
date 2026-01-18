package cool.muyucloud.croparia.api.crop.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ItemMaterial extends Material<Item> {
    public static final Codec<ItemMaterial> CODEC_STR = Codec.STRING.xmap(ItemMaterial::new, ItemMaterial::getName);
    public static final MapCodec<ItemMaterial> CODEC_COMP = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ItemMaterial::getName),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemMaterial::getComponents),
            Codec.INT.optionalFieldOf("count", 2).forGetter(ItemMaterial::getCount)
        ).apply(instance, ItemMaterial::new)
    );
    public static final MultiCodec<ItemMaterial> CODEC = CodecUtil.of(
        CodecUtil.of(CODEC_COMP.codec(), toDecode -> toDecode.components.equals(DataComponentPatch.EMPTY) && toDecode.getCount() == 2
            ? TestedCodec.fail(() -> "Count is 2 and no components, proceed to string codec") : TestedCodec.success()), CODEC_STR
    );
    public static final Placeholder<ItemMaterial> PLACEHOLDER = Placeholder.build(node -> node
        .self(TypeMapper.identity(), ItemMaterial.CODEC)
        .then(PatternKey.literal("result"), TypeMapper.of(material -> ItemOutput.of(material.asItem())), Placeholder.ITEM_OUTPUT)
        .then(PatternKey.literal("components"), TypeMapper.of(ItemMaterial::getComponents), Placeholder.DATA_COMPONENTS)
        .concat(Material.PLACEHOLDER, TypeMapper.of(material -> material)));

    @NotNull
    private final DataComponentPatch components;
    private transient final OnLoadSupplier<List<Item>> items = OnLoadSupplier.of(
        () -> this.candidates(BuiltInRegistries.ITEM.key().location()).stream().filter(
            item -> CropariaIf.CONFIG.isModValid(Objects.requireNonNull(item.arch$registryName()).getNamespace())
        ).toList()
    );

    public ItemMaterial(ItemStack stack) {
        this(String.valueOf(stack.getItem().arch$registryName()), stack.getComponentsPatch(), stack.getCount());
    }

    public ItemMaterial(@NotNull String name) {
        this(name, DataComponentPatch.EMPTY, 2);
    }

    public ItemMaterial(@NotNull String name, @NotNull DataComponentPatch components, int count) {
        super(name, count);
        this.components = components;
    }

    public @NotNull DataComponentPatch getComponents() {
        return components;
    }

    @Override
    public @NotNull ItemStack asItem() {
        List<Item> stacks = this.candidates();
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.candidates().getFirst().getDefaultInstance();
        stack.applyComponents(this.getComponents());
        stack.setCount(this.getCount());
        return stack;
    }

    @Override
    public List<Item> candidates() {
        return this.items.get();
    }
}
