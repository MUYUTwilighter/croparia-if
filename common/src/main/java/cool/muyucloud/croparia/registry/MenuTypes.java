package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.menu.CropTransmuterMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CropariaIf.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<CropTransmuterMenu>> CROP_TRANSMUTER = MENUS.register(
        "crop_transmuter",
        () -> MenuRegistry.ofExtended(CropTransmuterMenu::new)
    );

    public static void register() {
        CropariaIf.LOGGER.debug("Registering menu types");
        MENUS.register();
    }
}
