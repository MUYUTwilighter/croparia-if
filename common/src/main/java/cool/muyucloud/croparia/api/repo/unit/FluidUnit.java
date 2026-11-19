package cool.muyucloud.croparia.api.repo.unit;

import cool.muyucloud.croparia.api.resource.type.FluidSpec;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public interface FluidUnit extends Unit<FluidSpec> {
    @Override
    default @NonNull HalfDroplet baseUnit() {
        return Unit.HALF_DROPLET;
    }

    static FluidUnit create(final @NotNull String translationKey, final int ratio) {
        final long maxAmount = Long.MAX_VALUE / ratio;
        return new FluidUnit() {
            @Override
            public @NonNull String getTranslationKey() {
                return translationKey;
            }

            @Override
            public long toBase(long amount) {
                return Math.min(amount, maxAmount) * ratio;
            }

            @Override
            public @NonNull Result fromBase(long amount) {
                return new Result(amount / ratio, amount % ratio);
            }
        };
    }
}
