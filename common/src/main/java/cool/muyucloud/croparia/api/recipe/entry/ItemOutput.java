package cool.muyucloud.croparia.api.recipe.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ItemOutput implements SlotDisplay {
    public static final Codec<ItemOutput> CODEC_STR = ResourceLocation.CODEC.xmap(
        id -> new ItemOutput(id, 1), ItemOutput::getId
    );
    public static final MapCodec<ItemOutput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(ItemOutput::getId),
        DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(itemOutput -> Optional.of(itemOutput.getComponentsPatch())),
        Codec.LONG.optionalFieldOf("amount").forGetter(result -> Optional.of(result.getAmount()))
    ).apply(instance, (id, components, amount) -> new ItemOutput(id, components.orElse(DataComponentPatch.EMPTY), amount.orElse(1L))));
    public static final MultiCodec<ItemOutput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getComponentsPatch().isEmpty() && toEncode.getAmount() == 1L)
            return TestedCodec.fail(() -> "Can be encoded as string");
        return TestedCodec.success();
    }), CODEC_STR);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemOutput> STREAM_CODEC = CodecUtil.toStream(CODEC);
    public static final Type<ItemOutput> TYPE = new Type<>(CODEC_COMP, STREAM_CODEC);
    public static final ItemOutput EMPTY = new ItemOutput();

    public static ItemOutput of(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return EMPTY;
        return new ItemOutput(stack);
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final DataComponentPatch components;
    private final long amount;
    @NotNull
    private final transient ItemSpec itemSpec;
    @NotNull
    private final transient ItemStack displayStack;

    private ItemOutput() {
        this.id = BuiltInRegistries.ITEM.getKey(Items.AIR);
        this.components = DataComponentPatch.EMPTY;
        this.amount = 0;
        this.itemSpec = new ItemSpec(BuiltInRegistries.ITEM.getValue(this.id), this.components);
        this.displayStack = this.toSpec().createStack(this.getAmount());
    }

    public ItemOutput(@NotNull ItemStack stack) {
        this(Objects.requireNonNull(stack.getItem().arch$registryName()), stack.getComponentsPatch(), stack.getCount());
    }

    public ItemOutput(@NotNull ResourceLocation id, int amount) {
        this(id, DataComponentPatch.EMPTY, amount);
    }

    public ItemOutput(@NotNull ResourceLocation id, @NotNull DataComponentPatch components, long amount) {
        this.id = id;
        this.components = components;
        this.amount = amount;
        if (this.amount <= 0) CropariaIf.LOGGER.warn("Creating ItemOutput with non-positive amount: {}", this.amount);
        this.itemSpec = new ItemSpec(BuiltInRegistries.ITEM.getValue(id), components);
        if (this.itemSpec.isEmpty()) throw new IllegalArgumentException("Unknown or invalid item: " + id);
        this.displayStack = this.toSpec().createStack(this.getAmount());
    }

    public @NotNull ItemStack getDisplayStack() {
        return this.displayStack;
    }

    @NotNull
    public ResourceLocation getId() {
        return this.id;
    }

    @NotNull
    public DataComponentPatch getComponentsPatch() {
        return this.components;
    }

    public long getAmount() {
        return amount;
    }

    public @NotNull ItemSpec toSpec() {
        return itemSpec;
    }

    public ItemStack createStack() {
        return this.toSpec().createStack(getAmount());
    }

    @Override
    @NotNull
    public <T> Stream<T> resolve(ContextMap contextMap, DisplayContentsFactory<T> factory) {
        if (factory instanceof DisplayContentsFactory.ForStacks<T> forStacks) {
            return Stream.of(forStacks.forStack(this.getDisplayStack()));
        }
        return Stream.empty();
    }

    @Override
    @NotNull
    public Type<? extends SlotDisplay> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemOutput that)) return false;
        return amount == that.amount && Objects.equals(id, that.id) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, components, amount);
    }
}
