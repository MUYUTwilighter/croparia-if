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
import net.minecraft.core.Holder;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

@SuppressWarnings("unused")
public class ItemSpec implements DataComponentHolder, TypedResource<Item> {
    public static final MapCodec<ItemSpec> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(itemSpec -> itemSpec.getResource().arch$registryName()),
        CodecUtil.optionalFieldsOf(DataComponentPatch.CODEC, DataComponentPatch.EMPTY, "components", "nbt").forGetter(ItemSpec::getComponentsPatch)
    ).apply(instance, (id, components) -> new ItemSpec(BuiltInRegistries.ITEM.getValue(id), components)));
    public static final Codec<ItemSpec> CODEC_STR = Identifier.CODEC.xmap(
        id -> new ItemSpec(BuiltInRegistries.ITEM.getValue(id), DataComponentPatch.EMPTY),
        itemSpec -> itemSpec.getResource().arch$registryName()
    );
    public static final MultiCodec<ItemSpec> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getComponents().isEmpty()) return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final ItemSpec EMPTY = ItemSpec.of(Items.AIR);
    public static final TypeToken<ItemSpec> TYPE = TypeToken.register(CropariaIf.of("item_spec"), EMPTY, CODEC_COMP).orElseThrow();

    @NonNull
    private final Item resource;
    @NonNull
    private final PatchedDataComponentMap components;

    @NonNull
    public static ItemSpec of(@NonNull ItemStack stack) {
        return new ItemSpec(stack.getItem(), stack.getComponentsPatch());
    }

    @NonNull
    public static ItemSpec of(@NonNull Item item) {
        return new ItemSpec(item, DataComponentPatch.EMPTY);
    }

    public ItemSpec(@NonNull Item item) {
        this(item, DataComponentPatch.EMPTY);
    }

    public ItemSpec(@NonNull Item item, @NonNull DataComponentPatch components) {
        this.resource = item;
        this.components = PatchedDataComponentMap.fromPatch(item.components(), components);
    }

    public ItemSpec(@NonNull Item item, @NonNull DataComponentMap components) {
        this.resource = item;
        this.components = new PatchedDataComponentMap(item.components());
        components.forEach(typed -> typed.applyTo(this.components));
    }

    @NonNull
    public ItemSpec copy() {
        return new ItemSpec(this.getResource(), this.getComponentsPatch());
    }

    @NonNull
    public ItemSpec with(@NonNull Item item) {
        return new ItemSpec(item, this.getComponents());
    }

    @NonNull
    public ItemSpec with(@NonNull DataComponentPatch components) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        patched.applyPatch(components);
        return new ItemSpec(this.getResource(), patched);
    }

    @NonNull
    public ItemSpec with(@NonNull DataComponentMap components) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        patched.setAll(components);
        return new ItemSpec(this.getResource(), patched);
    }

    @NonNull
    public ItemSpec with(@NonNull TypedDataComponent<?> component) {
        PatchedDataComponentMap patched = new PatchedDataComponentMap(this.getComponents());
        component.applyTo(patched);
        return new ItemSpec(this.getResource(), patched);
    }

    @NonNull
    public <T> ItemSpec with(@NonNull DataComponentType<T> type, @NonNull T value) {
        return this.with(new TypedDataComponent<>(type, value));
    }

    @NonNull
    public ItemSpec replaceComponents(@NonNull DataComponentMap nbt) {
        return new ItemSpec(this.getResource(), nbt);
    }

    @NonNull
    public ItemStack createStack(long amount) {
        return new ItemStack(Holder.direct(this.getResource()), CifUtil.toIntSafe(amount), this.getComponentsPatch());
    }

    @NonNull
    public ItemStack createStack() {
        ItemStack stack = this.getResource().getDefaultInstance();
        stack.applyComponents(this.getComponents());
        return stack;
    }

    public boolean is(@NonNull ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, this.createStack());
    }

    public boolean is(@NonNull Identifier tag) {
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
    @NonNull
    public Item getResource() {
        return this.resource;
    }

    @Override
    @NonNull
    public DataComponentMap getComponents() {
        return this.components;
    }

    @NonNull
    public DataComponentPatch getComponentsPatch() {
        return this.components.asPatch();
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
