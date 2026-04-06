package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.network.CropTransmuterRedstoneModePacket;
import cool.muyucloud.croparia.api.core.network.CropTransmuterSelectPacket;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import dev.architectury.networking.NetworkManager;

@SuppressWarnings("unused")
public class NetworkHandlers {
    public static final NetworkHandlerType<CropTransmuterSelectPacket> CROP_TRANSMUTER = register(
        CropTransmuterSelectPacket.TYPE
    );
    public static final NetworkHandlerType<CropTransmuterRedstoneModePacket> CROP_TRANSMUTER_REDSTONE_MODE = register(
        CropTransmuterRedstoneModePacket.TYPE
    );

    public static <T extends NetworkHandler> NetworkHandlerType<T> register(NetworkHandlerType<T> type) {
        NetworkManager.registerReceiver(type.side(), type.id(), (buf, context) -> type.decode(buf).handle(context));
        return type;
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registered Croparia IF network handlers");
    }
}
