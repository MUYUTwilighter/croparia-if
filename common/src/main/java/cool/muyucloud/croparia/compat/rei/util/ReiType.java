package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import net.minecraft.world.item.crafting.RecipeInput;

public class ReiType<R extends DisplayableRecipe<?>> {
    public static <R extends DisplayableRecipe<?>> ReiType<R> of(TypedSerializer<R> type) {
        return new ReiType<>(type);
    }

    private final TypedSerializer<R> type;
    private final DisplaySerializer<ReiDisplay<R>> serializer;
    private final CategoryIdentifier<ReiDisplay<R>> id;

    public ReiType(TypedSerializer<R> type) {
        this.type = type;
        this.id = CategoryIdentifier.of(type.getId());
        this.serializer = ReiDisplay.serializer(this);
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends DisplayableRecipe<I>> ReiType<T> adapt() {
        return (ReiType<T>) this;
    }

    public TypedSerializer<R> getType() {
        return type;
    }

    public DisplaySerializer<ReiDisplay<R>> getSerializer() {
        return serializer;
    }

    public CategoryIdentifier<ReiDisplay<R>> getId() {
        return id;
    }
}
