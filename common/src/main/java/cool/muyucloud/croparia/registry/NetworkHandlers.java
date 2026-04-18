package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.network.CropTransmuterRedstoneModePacket;
import cool.muyucloud.croparia.api.core.network.CropTransmuterSelectPacket;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncRecipeChunk;
import dev.architectury.networking.NetworkManager;

@SuppressWarnings("unused")
public class NetworkHandlers {
    public static final NetworkHandlerType<CropTransmuterSelectPacket> CROP_TRANSMUTER = register(
        CropTransmuterSelectPacket.TYPE
    );
    public static final NetworkHandlerType<CropTransmuterRedstoneModePacket> CROP_TRANSMUTER_REDSTONE_MODE = register(
        CropTransmuterRedstoneModePacket.TYPE
    );
    public static final NetworkHandlerType<S2CSyncRecipeChunk> SYNC_RECIPE_CHUNK = register(S2CSyncRecipeChunk.TYPE);

    public static <T extends NetworkHandler> NetworkHandlerType<T> register(NetworkHandlerType<T> type) {
        if (type.side() == NetworkManager.Side.S2C) {
            CropariaIf.ifClientOrElse(
                client -> NetworkManager.registerReceiver(type.side(), type.type(), type.codec(), type.packetTransformers(), NetworkHandler::handle),
                mayServer -> NetworkManager.registerS2CPayloadType(type.type(), type.codec(), type.packetTransformers())
            );
        } else {
            NetworkManager.registerReceiver(type.side(), type.type(), type.codec(), type.packetTransformers(), NetworkHandler::handle);
        }
        return type;
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registered Croparia IF network handlers");
    }
}
