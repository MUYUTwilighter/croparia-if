package cool.muyucloud.croparia.client;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.recipe.sync.SyncedRecipeCache;
import cool.muyucloud.croparia.client.command.ClientCommandRoot;
import cool.muyucloud.croparia.client.screen.CropTransmuterScreen;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.DgRegistries;
import cool.muyucloud.croparia.registry.MenuTypes;
import cool.muyucloud.croparia.util.Ref;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.StemBlock;

public class CropariaIfClient {
    public static final int STEM_YOUNG = 0x4CAF50;
    public static final int STEM_MATURE = 0xB0852A;

    public static void init() {
        CropariaIf.LOGGER.info("Initializing client setup");
        ClientCommandRoot.register();
        CropariaIf.LOGGER.debug("Registering crop color");
        DgRegistries.CROPS.forLoaded(crop -> {
            // Crop Color
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, i) -> crop.getColor().getValue(), crop.getCropBlock().orElseThrow());
            RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, crop.getCropBlock().orElseThrow());
        });
        DgRegistries.MELONS.forLoaded(melon -> {
            // Melon Block Color
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    return melon.getColor().getValue();
                }
                return -1;
            }, melon.getMelon());
            // Stem Color
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
            }, melon.getStem());
            RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, melon.getStem().get());
            // Attach Color
            ColorHandlerRegistry.registerBlockColors((state, getter, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    return melon.getColor().getValue();
                }
                if (tintIndex == 1) {
                    return STEM_MATURE;
                }
                return -1;
            }, melon.getAttach());
            RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, melon.getAttach().get());
        });
        CropariaIf.LOGGER.debug("Registering cutout rendering");
        RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, CropariaBlocks.GREENHOUSE.get());
        RenderTypeRegistry.register(ChunkSectionLayer.CUTOUT, CropariaBlocks.ACTIVATED_SHRIEKER.get());
        MenuScreenRegistry.registerScreenFactory(MenuTypes.CROP_TRANSMUTER.get(), CropTransmuterScreen::new);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> SyncedRecipeCache.clearClient());
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> PackHandler.forEach(
            pack -> pack.onClientStopping(Ref.of(client))
        ));
    }
}
