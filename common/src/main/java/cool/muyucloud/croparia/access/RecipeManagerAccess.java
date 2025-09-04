package cool.muyucloud.croparia.access;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;

public interface RecipeManagerAccess {
    @Unique
    <I extends RecipeInput, R extends Recipe<I>> Collection<RecipeHolder<R>> cif$byType(RecipeType<R> type);
}
