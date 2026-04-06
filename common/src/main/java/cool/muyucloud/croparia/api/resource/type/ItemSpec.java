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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class ItemSpec implements TypedResource<Item> {
    public static final MapCodec<ItemSpec> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(itemSpec -> itemSpec.getResource().arch$registryName()),
        CodecUtil.optionalFieldsOf(CompoundTag.CODEC, new CompoundTag(), "components", "nbt").forGetter(ItemSpec::getTagOrEmpty)
    ).apply(instance, (id, tag) -> new ItemSpec(BuiltInRegistries.ITEM.get(id), tag.isEmpty() ? null : tag)));
    public static final Codec<ItemSpec> CODEC_STR = ResourceLocation.CODEC.xmap(
        id -> new ItemSpec(BuiltInRegistries.ITEM.get(id), null),
        itemSpec -> itemSpec.getResource().arch$registryName()
    );
    public static final MultiCodec<ItemSpec> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getTagOrEmpty().isEmpty()) return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final ItemSpec EMPTY = ItemSpec.of(Items.AIR);
    public static final TypeToken<ItemSpec> TYPE = TypeToken.register(CropariaIf.of("item_spec"), EMPTY, CODEC_COMP).orElseThrow();

    @NotNull
    private final Item resource;
    @Nullable
    private final CompoundTag tag;

    @NotNull
    public static ItemSpec of(@NotNull ItemStack stack) {
        return new ItemSpec(stack.getItem(), stack.getTag());
    }

    @NotNull
    public static ItemSpec of(@NotNull Item item) {
        return new ItemSpec(item, null);
    }

    public ItemSpec(@NotNull Item item) {
        this(item, null);
    }

    public ItemSpec(@NotNull Item item, @Nullable CompoundTag tag) {
        this.resource = item;
        this.tag = tag == null ? null : tag.copy();
    }

    @NotNull
    public ItemSpec copy() {
        return new ItemSpec(this.getResource(), this.tag);
    }

    @NotNull
    public ItemSpec with(@NotNull Item item) {
        return new ItemSpec(item, this.tag);
    }

    @NotNull
    public ItemSpec replaceNbt(@Nullable CompoundTag tag) {
        return new ItemSpec(this.getResource(), tag);
    }

    @NotNull
    public ItemStack createStack(long amount) {
        ItemStack stack = new ItemStack(this.getResource(), CifUtil.toIntSafe(amount));
        stack.setTag(this.tag == null ? null : this.tag.copy());
        return stack;
    }

    @NotNull
    public ItemStack createStack() {
        ItemStack stack = this.getResource().getDefaultInstance();
        stack.setTag(this.tag == null ? null : this.tag.copy());
        return stack;
    }

    public boolean is(@NotNull ItemStack stack) {
        return ItemStack.isSameItemSameTags(stack, this.createStack());
    }

    public boolean is(@NotNull ResourceLocation tag) {
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

    public Optional<CompoundTag> getNbt() {
        return Optional.ofNullable(this.tag == null ? null : this.tag.copy());
    }

    @NotNull
    public CompoundTag getTagOrEmpty() {
        return this.tag == null ? new CompoundTag() : this.tag.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemSpec itemSpec)) return false;
        if (this.isEmpty()) return itemSpec.isEmpty();
        return Objects.equals(resource, itemSpec.resource) && Objects.equals(tag, itemSpec.tag);
    }

    @Override
    public int hashCode() {
        if (this.isEmpty()) return 0;
        return Objects.hash(resource, tag);
    }
}
