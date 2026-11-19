package cool.muyucloud.croparia.api.repo.unit;

import cool.muyucloud.croparia.api.resource.TypedResource;
import org.jetbrains.annotations.NotNull;

public interface Unit<T extends TypedResource<?>> {
    /* Item Units */
    Count COUNT = new Count();
    /* Fluid Units */
    HalfDroplet HALF_DROPLET = new HalfDroplet();
    FluidUnit DROPLET = FluidUnit.create("unit.croparia.droplet", 2);
    FluidUnit MILL_BUCKET = FluidUnit.create("unit.croparia.millebucket", 162);

    @NotNull String getTranslationKey();

    /**
     * The base unit that is the highest precision in a unit system.
     */
    @NotNull Unit<T> baseUnit();

    /**
     * Calculates how much amount of the BASE unit can be transformed from the given amount of THIS unit.
     *
     * @param amount Amount of THIS unit
     * @return amount of BASE unit
     * @see #baseUnit()
     */
    long toBase(long amount);

    /**
     * Calculates how much amount of THIS unit can be transformed from the given amount of the BASE unit.
     *
     * @param amount Amount of BASE unit
     * @return amount of THIS unit with remainder of the BASE unit
     * @see #baseUnit()
     */
    @NotNull Result fromBase(long amount);

    /**
     * Transform the amount of the given unit to amount of THIS unit.
     *
     * @param amount amount of the given unit
     * @param unit   the unit for amount
     * @return amount of THIS unit with remainder of the BASE unit
     */
    default @NotNull Result from(long amount, @NotNull Unit<T> unit) {
        return fromBase(unit.toBase(amount));
    }

    /**
     * Transform the amount of the given unit to amount of THIS unit.
     *
     * @param amount amount of THIS unit
     * @param unit   the target unit
     * @return amount of the target unit and remainder of BASE unit
     */
    default @NotNull Result to(long amount, @NotNull Unit<T> unit) {
        return unit.fromBase(this.toBase(amount));
    }

    record Result(long amount, long remainder) {
    }
}
