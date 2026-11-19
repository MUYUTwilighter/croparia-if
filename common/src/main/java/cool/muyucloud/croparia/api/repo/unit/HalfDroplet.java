package cool.muyucloud.croparia.api.repo.unit;

import org.jspecify.annotations.NonNull;

/**
 * Base fluid unit for Repo API. 162000 half droplets = 1 bucket.
 */
public class HalfDroplet implements FluidUnit {
    protected HalfDroplet() {
    }

    @Override
    public @NonNull String getTranslationKey() {
        return "unit.croparia.half_droplet";
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
