package cool.muyucloud.croparia.api.recipe.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.TagUtil;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ItemInput implements SlotDisplay {
    public static final ItemInput EMPTY = new ItemInput(null, null, null, 0);
    public static final Codec<ItemInput> CODEC_STR = Codec.STRING.xmap(
        s -> s.isEmpty() ? EMPTY : new ItemInput(s, 1),
        ItemInput::getTaggable
    );
    public static final MapCodec<ItemInput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.optionalFieldOf("id").forGetter(ItemInput::getId),
        TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ItemInput::getTag),
        CodecUtil.optionalFieldsOf(CompoundTag.CODEC, "nbt", "components").forGetter(ItemInput::optionalComponents),
        Codec.LONG.optionalFieldOf("amount", 1L).forGetter(ItemInput::getAmount)
    ).apply(instance, (id, tag, components, amount) ->
        id.isEmpty() && tag.isEmpty() && components.isEmpty() || amount <= 0 ? EMPTY :
            new ItemInput(id.orElse(null), tag.orElse(null), components.orElse(null), amount)));
    public static final MultiCodec<ItemInput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.optionalComponents().isEmpty() && toEncode.getAmount() == 1L || toEncode.equals(EMPTY)) {
            return TestedCodec.fail(() -> "Can be encoded as string");
        }
        return TestedCodec.success();
    }), CODEC_STR);

    public static ItemInput of(ResourceLocation id) {
        return new ItemInput(id, null, null, 1L);
    }

    public static ItemInput ofTag(ResourceLocation id) {
        return new ItemInput(null, TagKey.create(Registries.ITEM, id), null, 1L);
    }

    public static ItemInput of(final ItemStack stack) {
        return new ItemInput(stack.getItem().arch$registryName(), null, stack.getTag(), stack.getCount());
    }

    @Nullable
    private final ResourceLocation id;
    @Nullable
    private final TagKey<Item> tag;
    @Nullable
    private final CompoundTag componentTag;
    private final long amount;
    private transient OnLoadSupplier<ImmutableList<ItemStack>> displayStacks;

    public ItemInput(String s, int amount) {
        this(s.startsWith("#") ? null : new ResourceLocation(s),
            s.startsWith("#") ? TagKey.create(Registries.ITEM, new ResourceLocation(s.substring(1))) : null,
            null, amount);
    }

    public ItemInput(@NotNull ResourceLocation id, int amount) {
        this(id, null, null, amount);
    }

    public ItemInput(@NotNull ItemStack stack) {
        this(stack.getItem().arch$registryName(), null, stack.getTag(), stack.getCount());
    }

    public ItemInput(@Nullable ResourceLocation id, @Nullable TagKey<Item> tag, @Nullable CompoundTag componentTag, long amount) {
        this.id = id;
        this.tag = tag;
        if (this.id != null && this.tag != null) {
            throw new IllegalArgumentException("id and tag cannot be both set");
        }
        this.componentTag = componentTag == null ? null : componentTag.copy();
        this.amount = amount;
        this.displayStacks = OnLoadSupplier.of(() -> {
            if (this.getId().isPresent()) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(this.getId().get()), (int) Math.min(Integer.MAX_VALUE, this.getAmount()));
                stack.setTag(this.componentTag == null ? null : this.componentTag.copy());
                if (stack.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Item with id '{}' not found, using placeholder", this.getId().get());
                    return ImmutableList.of(Texts.tooltip(BlockInput.STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.of(stack);
            } else if (this.getTag().isPresent()) {
                LinkedList<ItemStack> stacks = new LinkedList<>();
                TagUtil.forEntries(this.getTag().get()).forEach(entry -> {
                    ItemStack stack = new ItemStack(entry, (int) Math.min(Integer.MAX_VALUE, this.getAmount()));
                    stack.setTag(this.componentTag == null ? null : this.componentTag.copy());
                    stacks.addLast(stack);
                });
                if (stacks.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Item tag with id '{}' is empty, using placeholder", this.getTag().get().location());
                    return ImmutableList.of(Texts.tooltip(BlockInput.STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.copyOf(stacks);
            } else {
                ItemStack stack = new ItemStack(CropariaItems.PLACEHOLDER.get(), (int) Math.min(Integer.MAX_VALUE, this.getAmount()));
                stack.setTag(this.componentTag == null ? null : this.componentTag.copy());
                return ImmutableList.of(stack);
            }
        });
    }

    public String getTaggable() {
        return this.getTag().map(tag -> "#" + tag.location()).orElseGet(
            () -> this.getId().map(ResourceLocation::toString).orElse("")
        );
    }

    public Optional<ResourceLocation> getId() {
        return Optional.ofNullable(id);
    }

    public ResourceLocation getDisplayId() {
        return this.getTag().map(TagKey::location).orElse(this.getDisplayStacks().get(0).getItem().arch$registryName());
    }

    public Optional<TagKey<Item>> getTag() {
        return Optional.ofNullable(tag);
    }

    @Nullable
    public CompoundTag getComponentsPredicate() {
        return this.componentTag == null ? null : this.componentTag.copy();
    }

    public Optional<CompoundTag> optionalComponents() {
        return Optional.ofNullable(this.componentTag == null ? null : this.componentTag.copy());
    }

    public long getAmount() {
        return amount;
    }

    public ImmutableList<ItemStack> getDisplayStacks() {
        return this.displayStacks.get();
    }

    public void mapStacks(Function<ImmutableList<ItemStack>, ImmutableList<ItemStack>> mapper) {
        this.displayStacks = displayStacks.map(mapper);
    }

    public long consume(Iterable<ItemStack> stacks) {
        long remaining = this.getAmount();
        for (ItemStack stack : stacks) {
            if (this.matchType(stack)) {
                long count = stack.getCount();
                if (count >= remaining) {
                    stack.shrink(CifUtil.toIntSafe(remaining));
                    return 0;
                } else {
                    stack.setCount(0);
                    remaining -= count;
                }
            }
        }
        return remaining;
    }

    public boolean matches(Iterable<ItemStack> stacks) {
        long remaining = this.getAmount();
        for (ItemStack stack : stacks) {
            if (this.matchType(stack)) {
                long count = stack.getCount();
                if (count >= remaining) {
                    return true;
                }
                remaining -= count;
            }
        }
        return false;
    }

    public boolean matches(@NotNull Item item) {
        if (this.getId().isPresent()) {
            return this.getId().get().equals(item.arch$registryName());
        } else if (this.getTag().isPresent()) {
            return TagUtil.isIn(this.getTag().get(), item);
        }
        return true;
    }

    public boolean matches(@Nullable CompoundTag components) {
        if (this.componentTag == null || this.componentTag.isEmpty()) return true;
        return NbtUtils.compareNbt(this.componentTag, components, true);
    }

    public boolean matches(@NotNull ItemSpec item) {
        return this.matches(item.getResource()) && this.matches(item.getNbt().orElse(null));
    }

    public boolean matches(@NotNull ItemStack stack) {
        return this.matchType(stack) && this.getAmount() <= stack.getCount();
    }

    public boolean matchType(@NotNull ItemStack stack) {
        return this.matches(stack.getItem()) && this.matches(stack.getTag());
    }

    public boolean matches(@NotNull ItemSpec item, long amount) {
        return this.matches(item) && this.getAmount() <= amount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemInput itemInput)) return false;
        return amount == itemInput.amount && Objects.equals(id, itemInput.id) && Objects.equals(tag, itemInput.tag)
            && Objects.equals(componentTag, itemInput.componentTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tag, componentTag, amount);
    }
}
