package cool.muyucloud.croparia.api.resource.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.TagUtil;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("unused")
public class ItemSpec implements DataComponentHolder, TypedResource<Item> {
    public static final MapCodec<ItemSpec> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ItemSpec::getResource),
        CodecUtil.optionalFieldsOf(DataComponentPatch.CODEC, DataComponentPatch.EMPTY, "components", "nbt").forGetter(ItemSpec::getComponentsPatch)
    ).apply(instance, ItemSpec::new));
    public static final Codec<ItemSpec> CODEC_STR = BuiltInRegistries.ITEM.byNameCodec().xmap(ItemSpec::of, ItemSpec::getResource);
    public static final MultiCodec<ItemSpec> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getComponentsPatch().isEmpty()) return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final ItemSpec EMPTY = ItemSpec.of(Items.AIR);
    public static final TypeToken<ItemSpec> TYPE = TypeToken.register(CropariaIf.of("item_spec"), EMPTY, CODEC_COMP).orElseThrow();

    @NotNull
    private final Item resource;
    // Recipe decoding runs before item defaults are bound; retain only explicit overrides here.
    @NotNull
    private final DataComponentPatch components;

    @NotNull
    public static ItemSpec of(@NotNull ItemStack stack) {
        return new ItemSpec(stack.getItem(), stack.getComponentsPatch());
    }

    @NotNull
    public static ItemSpec of(@NotNull Item item) {
        return new ItemSpec(item, DataComponentPatch.EMPTY);
    }

    public ItemSpec(@NotNull Item item) {
        this(item, DataComponentPatch.EMPTY);
    }

    public ItemSpec(@NotNull Item item, @NotNull DataComponentPatch components) {
        this.resource = item;
        this.components = components;
    }

    public ItemSpec(@NotNull Item item, @NotNull DataComponentMap components) {
        this(item, DataComponentPatch.builder().set(components).build());
    }

    @NotNull
    public ItemSpec copy() {
        return new ItemSpec(this.getResource(), this.getComponentsPatch());
    }

    @NotNull
    public ItemSpec with(@NotNull Item item) {
        return new ItemSpec(item, this.getComponentsPatch());
    }

    @NotNull
    public ItemSpec with(@NotNull DataComponentPatch components) {
        DataComponentPatch.SplitResult existing = this.getComponentsPatch().split();
        DataComponentPatch.SplitResult incoming = components.split();
        DataComponentPatch.Builder builder = DataComponentPatch.builder().set(existing.added());
        existing.removed().forEach(builder::remove);
        builder.set(incoming.added());
        incoming.removed().forEach(builder::remove);
        return new ItemSpec(this.getResource(), builder.build());
    }

    @NotNull
    public ItemSpec with(@NotNull DataComponentMap components) {
        return this.with(DataComponentPatch.builder().set(components).build());
    }

    @NotNull
    public ItemSpec with(@NotNull TypedDataComponent<?> component) {
        return this.with(DataComponentPatch.builder().set(component).build());
    }

    @NotNull
    public <T> ItemSpec with(@NotNull DataComponentType<T> type, @NotNull T value) {
        return this.with(new TypedDataComponent<>(type, value));
    }

    @NotNull
    public ItemSpec replaceComponents(@NotNull DataComponentMap nbt) {
        return new ItemSpec(this.getResource(), nbt);
    }

    @NotNull
    public ItemStack createStack(long amount) {
        if (this.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(this.getResource()), CifUtil.toIntSafe(amount), this.getComponentsPatch());
    }

    @NotNull
    public ItemStack createStack() {
        return this.createStack(1);
    }

    public boolean is(@NotNull ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, this.createStack());
    }

    public boolean is(@NotNull Identifier tag) {
        return TagUtil.isIn(Registries.ITEM, tag, this.getResource());
    }

    @Override
    public TypeToken<ItemSpec> getType() {
        return TYPE;
    }

    @Override
    public MapCodec<ItemSpec> getCodec() {
        return CODEC_COMP;
    }

    @Override
    @NotNull
    public Item getResource() {
        return this.resource;
    }

    @Override
    @NotNull
    public DataComponentMap getComponents() {
        DataComponentMap defaults = this.resource == Items.AIR ? DataComponentMap.EMPTY : this.resource.components();
        return PatchedDataComponentMap.fromPatch(defaults, this.components);
    }

    @NotNull
    public DataComponentPatch getComponentsPatch() {
        return this.components;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemSpec itemSpec)) return false;
        if (this.isEmpty()) return itemSpec.isEmpty();
        return Objects.equals(resource, itemSpec.resource) && Objects.equals(components, itemSpec.components);
    }

    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;
        return Objects.hash(resource, components);
    }
}
