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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
    public static final ItemInput EMPTY = new ItemInput(null, null, DataComponentPatch.EMPTY, 0);
    public static final Codec<ItemInput> CODEC_STR = Codec.STRING.xmap(
        s -> s.isEmpty() ? EMPTY : new ItemInput(s, 1),
        ItemInput::getTaggable
    );
    public static final MapCodec<ItemInput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.optionalFieldOf("id").forGetter(ItemInput::getId),
        TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ItemInput::getTag),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemInput::getComponentsPatch),
        Codec.LONG.optionalFieldOf("amount", 1L).forGetter(ItemInput::getAmount)
    ).apply(instance, (id, tag, components, amount) ->
        id.isEmpty() && tag.isEmpty() && components.equals(DataComponentPatch.EMPTY) || amount <= 0 ? EMPTY :
            new ItemInput(id.orElse(null), tag.orElse(null), components, amount)));
    public static final MultiCodec<ItemInput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getComponentsPatch().equals(DataComponentPatch.EMPTY) && toEncode.getAmount() == 1 || toEncode.equals(EMPTY)) {
            return TestedCodec.fail(() -> "Can be encoded as string");
        } else {
            return TestedCodec.success();
        }
    }), CODEC_STR);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemInput> STREAM_CODEC = CodecUtil.toStream(CODEC);

    public static ItemInput of(Identifier id) {
        return new ItemInput(id, null, DataComponentPatch.EMPTY, 1L);
    }

    public static ItemInput ofTag(Identifier id) {
        return new ItemInput(null, TagKey.create(Registries.ITEM, id), DataComponentPatch.EMPTY, 1L);
    }

    public static ItemInput of(final ItemStack stack) {
        return new ItemInput(
            stack.getItem().arch$registryName(),
            null,
            CifUtil.extractPatch(stack.getComponentsPatch()),
            stack.getCount()
        );
    }

    @Nullable
    private final Identifier id;
    @Nullable
    private final TagKey<Item> tag;
    @NotNull
    private final DataComponentPatch componentsPatch;
    private final long amount;
    private transient OnLoadSupplier<ImmutableList<ItemStack>> displayStacks;

    public ItemInput(String s, int amount) {
        this(s.startsWith("#") ? null : Identifier.parse(s),
            s.startsWith("#") ? TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1))) : null,
            DataComponentPatch.EMPTY, amount);
    }

    public ItemInput(@NotNull Identifier id, int amount) {
        this(id, null, DataComponentPatch.EMPTY, amount);
    }

    public ItemInput(@NotNull ItemStack stack) {
        this(stack.getItem().arch$registryName(), null, CifUtil.extractPatch(stack.getComponentsPatch()), stack.getCount());
    }

    public ItemInput(@Nullable Identifier id, @Nullable TagKey<Item> tag,
                     @NotNull DataComponentPatch componentsPatch, long amount) {
        this.id = id;
        this.tag = tag;
        if (this.id != null && this.tag != null)
            throw new IllegalArgumentException("id and tag cannot be set at the same time");
        this.componentsPatch = componentsPatch;
        this.amount = amount;
        this.displayStacks = OnLoadSupplier.of(() -> {
            if (this.getId().isPresent()) {
                ItemStack stack = new ItemStack(Holder.direct(BuiltInRegistries.ITEM.getValue(this.getId().get())),
                    CifUtil.toIntSafe(this.getAmount()), this.getComponentsPatch());
                if (stack.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Item with id '{}' not found, using placeholder", this.getId().get());
                    return ImmutableList.of(Texts.tooltip(BlockInput.STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.of(stack);
            } else if (this.getTag().isPresent()) {
                LinkedList<ItemStack> stacks = new LinkedList<>();
                TagUtil.forEntries(this.getTag().get()).forEach(entry -> {
                    ItemStack stack = new ItemStack(entry, CifUtil.toIntSafe(this.getAmount()),
                        this.getComponentsPatch());
                    stacks.addLast(stack);
                });
                if (stacks.isEmpty()) {
                    DisplayableRecipe.LOGGER.error("Item tag with id '{}' is empty, using placeholder", this.getTag().get().location());
                    return ImmutableList.of(Texts.tooltip(BlockInput.STACK_UNKNOWN.copy(), Texts.literal(this.getTaggable())));
                }
                return ImmutableList.copyOf(stacks);
            } else {
                ItemStack stack = new ItemStack(Holder.direct(CropariaItems.PLACEHOLDER.get()),
                    CifUtil.toIntSafe(this.getAmount()),
                    this.getComponentsPatch());
                return ImmutableList.of(stack);
            }
        });
    }

    public String getTaggable() {
        return this.getTag().map(tag -> "#" + tag.location()).orElseGet(
            () -> this.getId().map(Identifier::toString).orElse("")
        );
    }

    public Optional<Identifier> getId() {
        return Optional.ofNullable(id);
    }

    public Identifier getDisplayId() {
        return this.getTag().map(TagKey::location).orElse(this.getDisplayStacks().getFirst().getItem().arch$registryName());
    }

    public Optional<TagKey<Item>> getTag() {
        return Optional.ofNullable(tag);
    }

    @NotNull
    public DataComponentPatch getComponentsPatch() {
        return this.componentsPatch;
    }

    public Optional<DataComponentPatch> optionalComponents() {
        if (this.componentsPatch.equals(DataComponentPatch.EMPTY)) return Optional.empty();
        return Optional.of(this.componentsPatch);
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
                } else {
                    remaining -= count;
                }
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

    public boolean matches(@NotNull DataComponentMap components) {
        return this.getComponentsPatch().equals(DataComponentPatch.EMPTY) || components.equals(this.getComponentsPatch().split().added());
    }

    /**
     * Whether the specified item matches the input, considering only the type of the item & components
     *
     * @param item The item to match
     * @return {@code true} if the specified and components item matches
     */
    public boolean matches(@NotNull ItemSpec item) {
        return this.matches(item.getResource()) && this.matches(item.getComponents());
    }

    /**
     * Whether the specified item stack matches the input, considering the type of the item, components and amount
     */
    public boolean matches(@NotNull ItemStack stack) {
        return this.matchType(stack) && this.getAmount() <= stack.getCount();
    }

    /**
     * Whether the specified item stack matches the input, considering the type of the item, components
     *
     */
    public boolean matchType(@NotNull ItemStack stack) {
        return this.matches(stack.getItem()) && this.matches(stack.getComponents());
    }

    public boolean matches(@NotNull ItemSpec item, long amount) {
        return this.matches(item) && this.getAmount() <= amount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemInput itemInput)) return false;
        return amount == itemInput.amount && Objects.equals(id, itemInput.id) && Objects.equals(tag, itemInput.tag)
            && Objects.equals(componentsPatch, itemInput.componentsPatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tag, componentsPatch, amount);
    }
}
