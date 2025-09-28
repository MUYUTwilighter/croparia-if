package cool.muyucloud.croparia.api.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.container.InfusorContainer;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.api.element.item.ElementalPotion;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.ItemInput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class InfusorRecipe implements DisplayableRecipe<InfusorContainer> {
    public static final TypedSerializer<InfusorRecipe> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("infusor"), InfusorRecipe.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ElementAccess.CODEC.fieldOf("element").forGetter(InfusorRecipe::getElement),
            ItemInput.CODEC.fieldOf("ingredient").forGetter(InfusorRecipe::getIngredient),
            ItemOutput.CODEC.fieldOf("result").forGetter(InfusorRecipe::getResult)
        ).apply(instance, InfusorRecipe::new)), TypedSerializer.JEI,
        Mappable.of(CropariaItems.INFUSOR, Item::getDefaultInstance)
    );
    public static final TypedSerializer<InfusorRecipe> OLD_TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("infusor_type"), InfusorRecipe.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ElementAccess.CODEC.fieldOf("element").forGetter(InfusorRecipe::getElement),
            ItemInput.CODEC.fieldOf("input").forGetter(InfusorRecipe::getIngredient),
            ItemOutput.CODEC.fieldOf("output").forGetter(InfusorRecipe::getResult),
            Codec.INT.fieldOf("count").forGetter(recipe -> Math.toIntExact(recipe.getResult().getAmount()))
        ).apply(instance, (element, input, output, count) ->
            new InfusorRecipe(element, input, new ItemOutput(output.getId(), output.getComponentsPatch(), count)))),
        TypedSerializer.JEI, Mappable.of(CropariaItems.INFUSOR, Item::getDefaultInstance)
    );

    protected final Element element;
    protected final ItemInput ingredient;
    protected final ItemOutput result;

    public InfusorRecipe(@NotNull Element element, @NotNull ItemInput ingredient, @NotNull ItemOutput result) {
        if (element == Element.EMPTY) throw new IllegalArgumentException("Element cannot be EMPTY");
        this.element = element;
        this.ingredient = ingredient;
        this.result = result;
        this.ingredient.mapStacks(stacks -> {
            stacks.forEach(stack -> Texts.tooltip(stack, Constants.ITEM_DROP_TOOLTIP));
            return stacks;
        });
    }

    public ItemInput getIngredient() {
        return ingredient;
    }

    public ItemOutput getResult() {
        return result;
    }

    public Element getElement() {
        return element;
    }

    public boolean matches(InfusorContainer container) {
        ItemStack input = container.getItem(0);
        return getIngredient().matches(input) && container.getElement() == getElement();
    }

    public @NotNull ItemStack assemble(@NotNull InfusorContainer container) {
        if (matches(container)) {
            ItemStack input = container.getItem(0);
            input.shrink((int) getIngredient().getAmount());
            return getResult().createStack();
        } else {
            return ItemStack.EMPTY;
        }
    }

    public ItemStack getPotion() {
        return Texts.tooltip(ElementalPotion.fromElement(getElement()).orElseThrow().getDefaultInstance(), Constants.ELEM_INFUSE_TOOLTIP);
    }

    @Override
    public @NotNull List<List<ItemStack>> getInputs() {
        return List.of(
            this.getIngredient().getDisplayStacks(),
            List.of(this.getPotion())
        );
    }

    public @NotNull List<List<ItemStack>> getOutputs() {
        return List.of(List.of(this.getResult().getDisplayStack()));
    }

    @Override
    public boolean matches(@NotNull InfusorContainer container, @Nullable Level level) {
        return matches(container);
    }

    @Override
    public @NotNull ItemStack assemble(InfusorContainer recipeInput, HolderLookup.Provider provider) {
        return assemble(recipeInput);
    }

    @Override
    @NotNull
    public ItemOutput result() {
        return this.getResult();
    }

    @Override
    @NotNull
    public SlotDisplay.ItemStackSlotDisplay craftingStation() {
        return new SlotDisplay.ItemStackSlotDisplay(CropariaItems.INFUSOR.get().getDefaultInstance());
    }

    @Override
    public TypedSerializer<? extends InfusorRecipe> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InfusorRecipe that)) return false;
        return Objects.equals(element, that.element) && Objects.equals(ingredient, that.ingredient) && Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, ingredient, result);
    }
}
