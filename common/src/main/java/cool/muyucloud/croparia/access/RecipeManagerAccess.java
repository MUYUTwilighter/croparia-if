package cool.muyucloud.croparia.access;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;

public interface RecipeManagerAccess {
    @Unique
    <C extends Container, R extends Recipe<C>> Collection<R> cif$byType(RecipeType<R> type);
}
