package cool.muyucloud.croparia.client;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.client.command.ClientCommandRoot;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.DgRegistries;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.StemBlock;

public class CropariaIfClient {
    public static final int STEM_YOUNG = 0x4CAF50;
    public static final int STEM_MATURE = 0xB0852A;

    public static void init() {
        CropariaIf.LOGGER.info("Initializing client setup");
        ClientCommandRoot.register();
        CropariaIf.LOGGER.debug("Registering crop color");
        DgRegistries.CROPS.forLoaded(crop -> {
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, i) -> crop.getColor().getValue(), crop.getCropBlock().orElseThrow());
            RenderTypeRegistry.register(RenderType.cutoutMipped(), crop.getCropBlock().orElseThrow());
        });
        DgRegistries.MELONS.forLoaded(melon -> {
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    return melon.getColor().getValue();
                }
                return -1;
            }, melon.getMelon().get());
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    int age = state.getValue(StemBlock.AGE);
                    float t = (float) age / StemBlock.MAX_AGE;
                    int r = (int) ((1 - t) * ((STEM_YOUNG >> 16) & 0xFF) + t * ((STEM_MATURE >> 16) & 0xFF));
                    int g = (int) ((1 - t) * ((STEM_YOUNG >> 8) & 0xFF) + t * ((STEM_MATURE >> 8) & 0xFF));
                    int b = (int) ((1 - t) * ((STEM_YOUNG) & 0xFF) + t * ((STEM_MATURE) & 0xFF));
                    return (r << 16) | (g << 8) | b;
                } else if (tintIndex == 1) {
                    return melon.getColor().getValue();
                }
                return -1;
            }, melon.getStem().get());
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    return melon.getColor().getValue();
                }
                return -1;
            }, melon.getAttach().get());
            RenderTypeRegistry.register(RenderType.cutoutMipped(), melon.getStem().get());
            RenderTypeRegistry.register(RenderType.cutoutMipped(), melon.getAttach().get());
        });
        CropariaIf.LOGGER.debug("Registering cutout rendering");
        RenderTypeRegistry.register(RenderType.cutout(), CropariaBlocks.GREENHOUSE.get());
        RenderTypeRegistry.register(RenderType.cutout(), CropariaBlocks.ACTIVATED_SHRIEKER.get());
    }
}
