package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.category.ReiCategory;
import cool.muyucloud.croparia.util.SidedRef;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;

import java.util.function.Function;

public class ProxyCategory<R extends DisplayableRecipe<?>> {
    private final TypedSerializer<R> type;
    private final DisplaySerializer<SimpleDisplay<R>> serializer;
    private final CategoryIdentifier<SimpleDisplay<R>> id;
    private final SidedRef<ReiCategory<R>> category;

    public ProxyCategory(TypedSerializer<R> type, Function<ProxyCategory<R>, ? extends ReiCategory<R>> category) {
        this.type = type;
        this.id = CategoryIdentifier.of(type.getId());
        this.serializer = SimpleDisplay.serializer(this);
        this.category = SidedRef.ofClient(() -> category.apply(this));
    }

    public TypedSerializer<R> getType() {
        return type;
    }

    public SidedRef<ReiCategory<R>> getCategory() {
        return category;
    }

    public DisplaySerializer<SimpleDisplay<R>> getSerializer() {
        return serializer;
    }

    public CategoryIdentifier<SimpleDisplay<R>> getId() {
        return id;
    }
}
