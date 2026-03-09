package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class LootConditions {
    private static final DeferredRegister<LootItemConditionType> REGISTRY = DeferredRegister.create(CropariaIf.MOD_ID, Registries.LOOT_CONDITION_TYPE);

    public static final RegistrySupplier<LootItemConditionType> BLOCK_INPUT = register("block_input", () -> BlockInput.CONDITION_TYPE);

    public static <T extends LootItemConditionType> RegistrySupplier<T> register(String path, Supplier<T> supplier) {
        return REGISTRY.register(path, supplier);
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering Loot item conditions (loot predicates) ");
        REGISTRY.register();
    }

}
