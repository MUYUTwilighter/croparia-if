package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.access.RecipeManagerAccess;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerAccess {
    @Shadow
    private RecipeMap recipes;

    @Unique
    @Override
    public <I extends RecipeInput, R extends Recipe<I>> Collection<RecipeHolder<R>> cif$byType(RecipeType<R> type) {
        return this.recipes.byType(type);
    }
}
