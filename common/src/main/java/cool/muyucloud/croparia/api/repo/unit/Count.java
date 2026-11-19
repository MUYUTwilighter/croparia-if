package cool.muyucloud.croparia.api.repo.unit;

import cool.muyucloud.croparia.api.resource.type.ItemSpec;
import org.jspecify.annotations.NonNull;

/**
 * Base item count unit.
 */
public class Count implements Unit<ItemSpec> {
    protected Count() {
    }

    @Override
    public @NonNull String getTranslationKey() {
        return "unit.croparia.count";
    }

    @Override
    public @NonNull Unit<ItemSpec> baseUnit() {
        return this;
    }

    @Override
    public long toBase(long amount) {
        return amount;
    }

    @Override
    public @NonNull Result fromBase(long amount) {
        return new Result(amount, 0);
    }
}
