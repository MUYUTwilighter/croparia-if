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
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class RitualRecipe implements DisplayableRecipe<RitualContainer> {
    public static final TypedSerializer<RitualRecipe> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("ritual"), RitualRecipe.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockInput.CODEC.fieldOf("ritual").forGetter(RitualRecipe::getRitual),
            BlockInput.CODEC.fieldOf("block").forGetter(RitualRecipe::getBlock),
            ItemInput.CODEC.fieldOf("ingredient").forGetter(RitualRecipe::getIngredient),
            ItemOutput.CODEC.fieldOf("result").forGetter(RitualRecipe::getResult)
        ).apply(instance, RitualRecipe::new)), TypedSerializer.JEI,
        Mappable.of(CropariaItems.RITUAL_STAND, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL)),
        Mappable.of(CropariaItems.RITUAL_STAND_2, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL)),
        Mappable.of(CropariaItems.RITUAL_STAND_3, item -> Texts.tooltip(item.getDefaultInstance(), Constants.TOOLTIP_RITUAL))
    );

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
        return List.of(List.of(this.getResult().getDisplayStack()));
    }

    public ItemStack assemble(RitualContainer matcher) {
        long consumed = 0;
        for (ItemStack stack : matcher.stacks()) {
            if (this.getIngredient().matchType(stack)) {
                long toConsume = Math.min(stack.getCount(), this.getIngredient().getAmount() - consumed);
                stack.shrink(Math.toIntExact(toConsume));
                consumed += toConsume;
            }
            if (consumed >= this.getIngredient().getAmount()) {
                return this.getResult().createStack();
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean matches(RitualContainer matcher) {
        long accumulated = 0;
        for (ItemStack stack : matcher.stacks()) {
            if (this.getIngredient().matchType(stack)) {
                accumulated += stack.getCount();
            }
            if (accumulated > this.getIngredient().getAmount()) {
                return matcher.matched().getStates().stream().allMatch(state -> this.getBlock().matches(state))
                    && this.getRitual().matches(matcher.ritual());
            }
        }
        return false;
    }

    @Override
    public boolean matches(RitualContainer container, Level level) {
        return matches(container);
    }

    @Override
    public @NotNull ItemStack assemble(RitualContainer recipeInput, HolderLookup.Provider provider) {
        return assemble(recipeInput);
    }

    @Override
    @NotNull
    public ItemOutput result() {
        return this.getResult();
    }

    @Override
    @NotNull
    public BlockInput craftingStation() {
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
