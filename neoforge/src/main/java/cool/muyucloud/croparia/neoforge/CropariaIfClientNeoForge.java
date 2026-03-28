package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.client.CropariaIfClient;
import cool.muyucloud.croparia.client.screen.CropTransmuterScreen;
import cool.muyucloud.croparia.neoforge.access.ArchitecturyFluidAttributesForgeAccess;
import cool.muyucloud.croparia.registry.MenuTypes;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@EventBusSubscriber(modid = CropariaIf.MOD_ID, value = Dist.CLIENT)
public class CropariaIfClientNeoForge {
    @SuppressWarnings("unchecked")
    public static final LazySupplier<Map<ArchitecturyFluidAttributes, FluidType>> FLUID_TYPES = CifUtil.forField(
        ArchitecturyFlowingFluid.class,
        "FLUID_TYPE_MAP"
    ).map(field -> {
        try {
            return (Map<ArchitecturyFluidAttributes, FluidType>) field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    });
    public static final LazySupplier<Constructor<FluidType>> FORGE_FLUID_ATTR_CONST = CifUtil.forConstructor(
        "dev.architectury.core.fluid.ArchitecturyFluidAttributesForge",
        FluidType.Properties.class, Fluid.class, ArchitecturyFluidAttributes.class
    );

    @SubscribeEvent()
    public static void onClientSetup(FMLClientSetupEvent event) {
        CropariaIfClient.init();
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Element element : Element.values()) {
            if (element.shouldLoad()) registerFluidClientExtensions(event, element.getFluidAttr());
        }
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypes.CROP_TRANSMUTER.get(), CropTransmuterScreen::new);
    }

    private static void registerFluidClientExtensions(
        RegisterClientExtensionsEvent event,
        ArchitecturyFluidAttributes attributes
    ) {
        FluidType type = FLUID_TYPES.get().computeIfAbsent(attributes, attr -> {
            try {
                return FORGE_FLUID_ATTR_CONST.get().newInstance(FluidType.Properties.create(), attributes.getSourceFluid(), attributes);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        if (type instanceof ArchitecturyFluidAttributesForgeAccess access) {
            event.registerFluidType(access.cif$getExtension(), type);
        }
    }
}
