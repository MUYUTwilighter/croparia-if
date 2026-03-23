package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.network.MaterialExtractorSelectPacket;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import dev.architectury.networking.NetworkManager;

@SuppressWarnings("unused")
public class NetworkHandlers {
    public static final NetworkHandlerType<MaterialExtractorSelectPacket> MATERIAL_EXTRACTOR_SELECT = register(
        MaterialExtractorSelectPacket.TYPE
    );

    public static <T extends NetworkHandler> NetworkHandlerType<T> register(NetworkHandlerType<T> type) {
        if (type.side() == NetworkManager.Side.S2C) {
            CropariaIf.ifClientOrElse(
                client -> NetworkManager.registerReceiver(type.side(), type.type(), type.codec(), NetworkHandler::handle),
                mayServer -> NetworkManager.registerS2CPayloadType(type.type(), type.codec())
            );
        } else {
            NetworkManager.registerReceiver(type.side(), type.type(), type.codec(), NetworkHandler::handle);
        }
        return type;
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registered Croparia IF network handlers");
    }
}
