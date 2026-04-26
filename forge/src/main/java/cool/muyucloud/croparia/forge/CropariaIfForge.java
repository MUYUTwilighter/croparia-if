package cool.muyucloud.croparia.forge;

import com.electronwill.nightconfig.toml.TomlParser;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.repo.forge.ProxyProviderImpl;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CropariaIf.MOD_ID)
@Mod.EventBusSubscriber(modid = CropariaIf.MOD_ID)
public class CropariaIfForge {
    public static final TomlParser TOML_PARSER = new TomlParser();

    public CropariaIfForge(FMLJavaModLoadingContext context) {
        EventBuses.registerModEventBus(CropariaIf.MOD_ID, context.getModEventBus());
        CropariaIf.init();
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        ProxyProviderImpl.freeze();
    }
}
