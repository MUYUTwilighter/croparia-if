package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

public class CropariaFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create("croparia", Registries.FLUID);

    public static <T extends Fluid> RegistrySupplier<T> registerFluid(ResourceLocation id, Supplier<T> supplier) {
        return FLUIDS.register(id, supplier);
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering fluids");
        FLUIDS.register();
    }
}
