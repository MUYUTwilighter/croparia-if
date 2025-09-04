package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.network.NetworkHandler;
import cool.muyucloud.croparia.api.network.NetworkHandlerType;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncClear;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncRecipe;
import dev.architectury.networking.NetworkManager;

@SuppressWarnings("unused")
public class NetworkHandlers {
    public static final NetworkHandlerType<S2CSyncClear<?>> SYNC_CLEAR = register(S2CSyncClear.TYPE);
    public static final NetworkHandlerType<S2CSyncRecipe<?>> SYNC_RECIPE = register(S2CSyncRecipe.TYPE);

    public static <T extends NetworkHandler> NetworkHandlerType<T> register(NetworkHandlerType<T> type) {
        NetworkManager.registerReceiver(type.side(), type.type(), type.codec(), NetworkHandler::handle);
        return type;
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registered Croparia IF network handlers");
    }
}
