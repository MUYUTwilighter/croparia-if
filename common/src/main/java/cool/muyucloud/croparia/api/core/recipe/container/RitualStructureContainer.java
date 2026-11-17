package cool.muyucloud.croparia.api.core.recipe.container;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public class RitualStructureContainer implements RecipeInput {
    private final BlockInWorld block;

    public RitualStructureContainer(BlockInWorld block) {
        this.block = block;
    }

    public BlockInWorld getBlock() {
        return block;
    }

    public BlockState getState() {
        return block.getState();
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return this.getState() == null || this.getState().isAir();
    }
}
