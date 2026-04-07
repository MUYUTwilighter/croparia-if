package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.RecipeManagerAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.List;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerAccess {
    @Shadow
    public abstract <C extends Container, T extends Recipe<C>> List<T> getAllRecipesFor(RecipeType<T> recipeType);

    @Unique
    @Override
    public <C extends Container, R extends Recipe<C>> Collection<R> cif$byType(RecipeType<R> type) {
        return this.getAllRecipesFor(type);
    }
}
