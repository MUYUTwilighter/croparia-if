package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.asset.CropItemSelectProperty;
import cool.muyucloud.croparia.api.crop.asset.CropItemTintSource;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.client.CropariaIfClient;
import cool.muyucloud.croparia.client.screen.CropTransmuterScreen;
import cool.muyucloud.croparia.registry.MenuTypes;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

@EventBusSubscriber(modid = CropariaIf.MOD_ID, value = Dist.CLIENT)
public class CropariaIfClientNeoForge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CropariaIfClient.init();
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Element element : Element.values()) {
            if (element == Element.EMPTY) continue;
            event.registerFluidType(new CropariaFluidTypeExtension(element.getFluidAttr()), element.getFluidSource().get().getFluidType());
        }
    }

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        for (Element element : Element.values()) {
            if (element == Element.EMPTY) continue;
            var attributes = element.getFluidAttr();
            var overlay = attributes.getOverlayTexture();
            event.register(new FluidModel.Unbaked(
                new Material(attributes.getSourceTexture(), true),
                new Material(attributes.getFlowingTexture(), true),
                overlay == null ? null : new Material(overlay, true),
                FluidTintSources.constant(attributes.getColor())
            ), element.getFluidSource(), element.getFluidFlowing());
        }
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypes.CROP_TRANSMUTER.get(), CropTransmuterScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockTint(RegisterColorHandlersEvent.BlockTintSources event) {
        CropariaIfClient.registerBlockTintSources(event::register);
    }

    @SubscribeEvent
    public static void onRegisterItemTint(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(CropariaIf.of("crop"), CropItemTintSource.CODEC);
    }

    @SubscribeEvent
    public static void onRegisterSelectItemModelProperty(RegisterSelectItemModelPropertyEvent event) {
        event.register(CropariaIf.of("crop_type"), CropItemSelectProperty.TYPE);
    }
}
