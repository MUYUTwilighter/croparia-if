package cool.muyucloud.croparia.api.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.container.SoakContainer;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.Mappable;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SoakRecipe implements DisplayableRecipe<SoakContainer> {
    public static final TypedSerializer<SoakRecipe> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("soak"), SoakRecipe.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Element.CODEC.fieldOf("element").forGetter(SoakRecipe::getElement),
            Codec.FLOAT.fieldOf("probability").forGetter(SoakRecipe::getProbability),
            BlockInput.codec(stack -> CifUtil.addTooltip(stack, Constants.SOAK_BLOCK_INPUT)).fieldOf("input").forGetter(SoakRecipe::getInput),
            BlockOutput.CODEC.fieldOf("output").forGetter(SoakRecipe::getOutput)
        ).apply(instance, SoakRecipe::new)),
        Mappable.of(CropariaItems.ELEMENTAL_STONE, Item::getDefaultInstance)
    );

    private final Element element;
    private final float probability;
    private final BlockInput input;
    private final BlockOutput output;

    public SoakRecipe(Element element, float probability, BlockInput input, BlockOutput output) {
        this.element = element;
        this.probability = probability;
        this.input = input;
        this.output = output;
    }

    public Element getElement() {
        return element;
    }

    public ItemStack getPotion() {
        return CifUtil.addTooltip(this.getElement().getPotion().get().getDefaultInstance(), Constants.ELEM_INFUSE_TOOLTIP);
    }

    public float getProbability() {
        return probability;
    }

    public BlockInput getInput() {
        return input;
    }

    public BlockOutput getOutput() {
        return output;
    }

    @Override
    public @NotNull List<List<ItemStack>> getInputs() {
        return List.of(this.getInput().getDisplayStacks());
    }

    @Override
    public @NotNull List<List<ItemStack>> getOutputs() {
        return List.of(List.of(this.getOutput().getDisplayStack()));
    }

    @Override
    public boolean matches(SoakContainer input, Level level) {
        return this.getInput().matches(input.getState()) && this.getElement() == input.getElement() && input.getRandom() < this.getProbability();
    }

    @Override
    public TypedSerializer<SoakRecipe> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    @Deprecated
    public @NotNull ItemStack assemble(SoakContainer recipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull SlotDisplay result() {
        return this.getOutput();
    }

    @Override
    public @NotNull SlotDisplay craftingStation() {
        return new SlotDisplay.ItemStackSlotDisplay(this.getTypedSerializer().getStations().getFirst().get());
    }
}
