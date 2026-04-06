package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;

public class ReiType<R extends DisplayableRecipe<?>> {
    public static <R extends DisplayableRecipe<?>> ReiType<R> of(TypedSerializer<R> type) {
        return new ReiType<>(type);
    }

    private final TypedSerializer<R> type;
    private final CategoryIdentifier<ReiDisplay<R>> id;

    public ReiType(TypedSerializer<R> type) {
        this.type = type;
        this.id = CategoryIdentifier.of(type.getId());
    }

    @SuppressWarnings("unchecked")
    public <T extends DisplayableRecipe<?>> ReiType<T> adapt() {
        return (ReiType<T>) this;
    }

    public TypedSerializer<R> getType() {
        return type;
    }

    public CategoryIdentifier<ReiDisplay<R>> getId() {
        return id;
    }
}
