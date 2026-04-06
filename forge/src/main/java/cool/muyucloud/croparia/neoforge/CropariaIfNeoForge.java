package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.repo.neoforge.ProxyProviderImpl;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CropariaIf.MOD_ID)
@Mod.EventBusSubscriber(modid = CropariaIf.MOD_ID)
public class CropariaIfNeoForge {
    public CropariaIfNeoForge() {
        EventBuses.registerModEventBus(CropariaIf.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CropariaIf.init();
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        ProxyProviderImpl.freeze();
    }
}
