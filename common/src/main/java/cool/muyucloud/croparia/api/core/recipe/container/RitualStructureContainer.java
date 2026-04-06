package cool.muyucloud.croparia.api.core.recipe.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public class RitualStructureContainer implements Container {
    private final BlockState state;

    public RitualStructureContainer(BlockState state) {
        this.state = state;
    }

    public BlockState getState() {
        return state;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return state.isAir();
    }

    @Override
    public @NotNull ItemStack removeItem(int i, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int i, @NotNull ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }
}
