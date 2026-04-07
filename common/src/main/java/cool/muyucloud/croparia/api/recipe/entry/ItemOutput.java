package cool.muyucloud.croparia.api.recipe.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.codec.MultiCodec;
import cool.muyucloud.croparia.api.codec.TestedCodec;
import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class ItemOutput implements SlotDisplay {
    public static final Codec<ItemOutput> CODEC_STR = ResourceLocation.CODEC.xmap(
        id -> new ItemOutput(id, 1), ItemOutput::getId
    );
    public static final MapCodec<ItemOutput> CODEC_COMP = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("id").forGetter(ItemOutput::getId),
        CodecUtil.optionalFieldsOf(CompoundTag.CODEC, "nbt", "components").forGetter(itemOutput -> Optional.of(itemOutput.getNbt())),
        Codec.LONG.optionalFieldOf("amount").forGetter(result -> Optional.of(result.getAmount()))
    ).apply(instance, (id, components, amount) -> new ItemOutput(id, components.orElse(new CompoundTag()), amount.orElse(1L))));
    public static final MultiCodec<ItemOutput> CODEC = CodecUtil.of(CodecUtil.of(CODEC_COMP.codec(), toEncode -> {
        if (toEncode.getNbt().isEmpty() && toEncode.getAmount() == 1L) {
            return TestedCodec.fail(() -> "Can be encoded as string");
        }
        return TestedCodec.success();
    }), CODEC_STR);
    public static final ItemOutput EMPTY = new ItemOutput();

    public static ItemOutput of(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return EMPTY;
        return new ItemOutput(stack);
    }

    @NotNull
    private final ResourceLocation id;
    @NotNull
    private final CompoundTag components;
    private final long amount;
    @NotNull
    private final transient ItemSpec itemSpec;
    @NotNull
    private final transient ItemStack displayStack;

    private ItemOutput() {
        this.id = BuiltInRegistries.ITEM.getKey(Items.AIR);
        this.components = new CompoundTag();
        this.amount = 0;
        this.itemSpec = new ItemSpec(BuiltInRegistries.ITEM.get(this.id), this.components);
        this.displayStack = this.toSpec().createStack(this.getAmount());
    }

    public ItemOutput(@NotNull ItemStack stack) {
        this(Objects.requireNonNull(stack.getItem().arch$registryName()), stack.getTag(), stack.getCount());
    }

    public ItemOutput(@NotNull ResourceLocation id, int amount) {
        this(id, new CompoundTag(), amount);
    }

    public ItemOutput(@NotNull ResourceLocation id, CompoundTag components, long amount) {
        this.id = id;
        this.components = components == null ? new CompoundTag() : components.copy();
        this.amount = amount;
        if (this.amount <= 0) CropariaIf.LOGGER.warn("Creating ItemOutput with non-positive amount: {}", this.amount);
        this.itemSpec = new ItemSpec(BuiltInRegistries.ITEM.get(id), this.components);
        if (this.itemSpec.isEmpty()) throw new IllegalArgumentException("Unknown or invalid item: " + id);
        this.displayStack = this.toSpec().createStack(this.getAmount());
    }

    public @NotNull List<ItemStack> getDisplayStacks() {
        return List.of(this.displayStack);
    }

    @NotNull
    public ResourceLocation getId() {
        return this.id;
    }

    @NotNull
    public CompoundTag getNbt() {
        return this.components.copy();
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
    public boolean equals(Object o) {
        if (!(o instanceof ItemOutput that)) return false;
        return amount == that.amount && Objects.equals(id, that.id) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, components, amount);
    }
}
