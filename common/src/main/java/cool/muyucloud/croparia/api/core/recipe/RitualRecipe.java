package cool.muyucloud.croparia.api.core.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.container.RitualContainer;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.entry.ItemInput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RitualRecipe implements DisplayableRecipe<RitualContainer> {
    public static final TypedSerializer<RitualRecipe> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("ritual"), RitualRecipe.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockInput.CODEC.fieldOf("ritual").forGetter(RitualRecipe::getRitual),
            BlockInput.CODEC.fieldOf("block").forGetter(RitualRecipe::getBlock),
            ItemInput.CODEC.fieldOf("ingredient").forGetter(RitualRecipe::getIngredient),
            ItemOutput.CODEC.fieldOf("result").forGetter(RitualRecipe::getResult)
        ).apply(instance, RitualRecipe::new)),
        Mappable.of(CropariaItems.RITUAL_STAND, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL)),
        Mappable.of(CropariaItems.RITUAL_STAND_2, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL)),
        Mappable.of(CropariaItems.RITUAL_STAND_3, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL))
    );
    public static final Style ENCHANTS = Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE);

    private final BlockInput ritual;
    @NotNull
    private final BlockInput block;
    @NotNull
    private final ItemInput ingredient;
    @NotNull
    private final ItemOutput result;

    public RitualRecipe(
        BlockInput ritual, @NotNull BlockInput state, @NotNull ItemInput ingredient, @NotNull ItemOutput result
    ) {
        this.ritual = ritual;
        this.block = state;
        this.ingredient = ingredient;
        this.result = result;
        this.ritual.mapStacks(stacks -> {
            stacks.forEach(stack -> Texts.tooltip(stack, Constants.TOOLTIP_RITUAL));
            return stacks;
        });
        this.ingredient.mapStacks(stacks -> {
            stacks.forEach(stack -> Texts.tooltip(stack, Constants.ITEM_DROP_TOOLTIP));
            return stacks;
        });
        this.block.mapStacks(stacks -> {
            stacks.forEach(stack -> Texts.tooltip(stack, Constants.BLOCK_PLACE_TOOLTIP));
            return stacks;
        });
        this.result.getDisplayStacks().forEach(stack -> {
            if (stack.getItem() instanceof SpawnEggItem) {
                Texts.tooltip(stack, Texts.translatable("tooltip.croparia.spawn_egg"));
            } else if (stack.getItem() == Items.ENCHANTED_BOOK && this.getIngredient().getAmount() == 1L) {
                Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
                if (enchants.isEmpty()) return;
                Texts.tooltip(stack, Texts.translatable("tooltip.croparia.ritual.enchant.header").withStyle(ENCHANTS));
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    Integer level = entry.getValue();
                    Texts.tooltip(stack, Texts.translatable(
                        "tooltip.croparia.ritual.enchant.entry",
                        Texts.translatable(enchant.getDescriptionId()),
                        stack.getCount(),
                        level
                    ).withStyle(ENCHANTS));
                }
            }
        });
    }

    public @NotNull ItemInput getIngredient() {
        return ingredient;
    }

    public @NotNull ItemOutput getResult() {
        return result;
    }

    public @NotNull BlockInput getBlock() {
        return block;
    }

    public @NotNull BlockInput getRitual() {
        return this.ritual;
    }

    @Override
    public @NotNull List<List<ItemStack>> getInputs() {
        return List.of(
            this.getIngredient().getDisplayStacks(),
            this.getBlock().getDisplayStacks()
        );
    }

    @Override
    public @NotNull List<List<ItemStack>> getOutputs() {
        List<ItemStack> results = this.getResult().getDisplayStacks();
        ItemStack stack = results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
        return List.of(List.of(stack));
    }

    public ItemStack assemble(RitualContainer matcher) {
        ItemStack result = this.getResult().createStack();
        // Handle enchanted book special case
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(result);
        if (this.getIngredient().getAmount() == 1L && result.getItem() == Items.ENCHANTED_BOOK) {
            for (ItemStack stack : matcher.stacks()) {
                if (this.getIngredient().matchType(stack)) {
                    matcher.matched().destroy();
                    ItemStack toEnchant = stack.copyWithCount(1);
                    stack.shrink(1);
                    Map<Enchantment, Integer> applied = EnchantmentHelper.getEnchantments(toEnchant);
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        int level = applied.getOrDefault(entry.getKey(), 0);
                        applied.put(entry.getKey(), Math.min(level + result.getCount(), entry.getValue()));
                    }
                    EnchantmentHelper.setEnchantments(applied, toEnchant);
                    return toEnchant;
                }
            }
            return ItemStack.EMPTY;
        }
        // Handle common case
        long consumed = 0;
        for (ItemStack stack : matcher.stacks()) {
            if (this.getIngredient().matchType(stack)) {
                long toConsume = Math.min(stack.getCount(), this.getIngredient().getAmount() - consumed);
                stack.shrink(Math.toIntExact(toConsume));
                consumed += toConsume;
            }
            if (consumed >= this.getIngredient().getAmount()) {
                matcher.matched().destroy();
                return this.getResult().createStack();
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean matches(RitualContainer matcher) {
        ItemStack result = this.getResult().createStack();
        if (!this.getRitual().matches(matcher.ritual())) {
            return false;
        }
        // Handle enchanted book special case
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(result);
        if (this.getIngredient().getAmount() == 1L && result.getItem() == Items.ENCHANTED_BOOK && !enchantments.isEmpty()) {
            return matcher.stacks().stream().anyMatch(stack -> {
                Map<Enchantment, Integer> toCheck = EnchantmentHelper.getEnchantments(stack);
                return enchantments.entrySet().stream().anyMatch(entry -> toCheck.getOrDefault(entry.getKey(), 0) < entry.getValue())
                    && this.getIngredient().matches(stack);
            }) && matcher.matched().getStates().stream().allMatch(state -> this.getBlock().matches(state));
        }
        // Handle common case
        return this.getIngredient().matches(matcher) && matcher.matched().getStates().stream().allMatch(state -> this.getBlock().matches(state));
    }

    @Override
    public boolean matches(RitualContainer container, Level level) {
        return matches(container);
    }

    @Override
    public @NotNull ItemStack assemble(RitualContainer recipeInput, RegistryAccess registryAccess) {
        return assemble(recipeInput);
    }

    @Override
    public @NotNull BlockInput craftingStation() {
        return this.getRitual();
    }

    @Override
    public TypedSerializer<? extends DisplayableRecipe<RitualContainer>> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RitualRecipe that)) return false;
        return Objects.equals(ritual, that.ritual) && Objects.equals(block, that.block) && Objects.equals(ingredient, that.ingredient) && Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ritual, block, ingredient, result);
    }
}
