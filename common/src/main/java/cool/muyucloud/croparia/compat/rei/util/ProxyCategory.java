package cool.muyucloud.croparia.compat.rei.util;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.category.SimpleCategory;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.platform.Platform;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import net.fabricmc.api.EnvType;

import java.util.function.Function;
import java.util.function.Supplier;

public class ProxyCategory<R extends DisplayableRecipe<?>> {
    public static <R extends DisplayableRecipe<?>> ProxyCategory<R> of(TypedSerializer<R> type, Function<ProxyCategory<R>, ? extends SimpleCategory<R>> category) {
        return new ProxyCategory<>(type, category);
    }

    private final TypedSerializer<R> type;
    private final DisplaySerializer<SimpleDisplay<R>> serializer;
    private final CategoryIdentifier<SimpleDisplay<R>> id;
    private final LazySupplier<? extends SimpleCategory<R>> category;

    public ProxyCategory(TypedSerializer<R> type, Function<ProxyCategory<R>, ? extends SimpleCategory<R>> category) {
        this.type = type;
        this.id = CategoryIdentifier.of(type.getId().orElseThrow());
        this.serializer = SimpleDisplay.serializer(this);
        this.category = LazySupplier.of(() -> {
            if (Platform.getEnv() == EnvType.SERVER) {
                return null;
            } else {
                return category.apply(this);
            }
        });
    }

    public TypedSerializer<R> getType() {
        return type;
    }

    public Supplier<? extends SimpleCategory<R>> getCategory() {
        return category;
    }

    public DisplaySerializer<SimpleDisplay<R>> getSerializer() {
        return serializer;
    }

    public CategoryIdentifier<SimpleDisplay<R>> getId() {
        return id;
    }
}
