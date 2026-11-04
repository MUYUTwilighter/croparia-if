package cool.muyucloud.croparia.api.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class DisplayableRecipeType<R extends Recipe<?>> implements RecipeType<R> {
    private final Identifier id;

    public DisplayableRecipeType(Identifier id) {
        this.id = id;
    }

    public Identifier getId() {
        return id;
    }
}
