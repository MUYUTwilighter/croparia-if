package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class Tabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CropariaIf.MOD_ID, Registries.CREATIVE_MODE_TAB);

    @SuppressWarnings("UnstableApiUsage")
    public static final RegistrySupplier<CreativeModeTab> CROPS = registerTab(
        "crops",
        () -> CreativeTabRegistry.create(builder -> builder
            .title(Texts.translatable("tab." + CropariaIf.MOD_ID + ".crops"))
            .icon(() -> {
                ItemStack stack = CropariaItems.CROP_FRUIT.get().getDefaultInstance();
                stack.set(CropariaComponents.CROP_ID.get(), Crops.ELEMENTAL.getKey());
                return stack;
            })
            .displayItems((_, output) -> DgRegistries.CROPS.forEach(crop -> {
                output.accept(crop.getFruit());
                output.accept(crop.getSeed());
            }))
        )
    );
    // The initializer registers this tab even without a direct Java reference.
    @SuppressWarnings({"unused", "UnstableApiUsage"})
    public static final RegistrySupplier<CreativeModeTab> MELONS = registerTab(
        "melons",
        () -> CreativeTabRegistry.create(builder -> builder
            .title(Texts.translatable("tab." + CropariaIf.MOD_ID + ".melons"))
            .icon(() -> {
                ItemStack stack = CropariaItems.MELON_ITEM.get().getDefaultInstance();
                stack.set(CropariaComponents.CROP_ID.get(), Melons.COBBLESTONE.getKey());
                return stack;
            })
            .displayItems((_, output) -> DgRegistries.MELONS.forEach(melon -> {
                output.accept(melon.getFruit());
                output.accept(melon.getSeed());
            }))
        )
    );
    public static final RegistrySupplier<CreativeModeTab> MAIN = registerTab(
        "main",
        () -> CreativeTabRegistry.create(
            Texts.translatable("tab." + CropariaIf.MOD_ID + ".main"),
            () -> Element.ELEMENTAL.getGem().get().getDefaultInstance()
        )
    );

    public static RegistrySupplier<CreativeModeTab> registerTab(String name, Supplier<CreativeModeTab> supplier) {
        return TABS.register(name, supplier);
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering tabs");
        TABS.register();
    }

    public static CreativeModeTab get(String id) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.tryBuild(CropariaIf.MOD_ID, id));
    }
}
