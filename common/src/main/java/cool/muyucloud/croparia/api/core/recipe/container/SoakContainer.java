package cool.muyucloud.croparia.api.core.recipe.container;

import cool.muyucloud.croparia.api.element.Element;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;

public class SoakContainer implements RecipeInput {
    private final BlockInWorld block;
    private final Element element;
    private final float random;

    public SoakContainer(BlockInWorld block, Element element, float random) {
        this.block = block;
        this.element = element;
        this.random = random;
    }

    public BlockInWorld getBlock() {
        return block;
    }

    public BlockState getState() {
        return block.getState();
    }

    public Element getElement() {
        return element;
    }

    public float getRandom() {
        return random;
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return this.getState() == null || this.getState().isAir() || element == Element.EMPTY || random <= 0;
    }

    @Override
    public int size() {
        return 0;
    }
}
