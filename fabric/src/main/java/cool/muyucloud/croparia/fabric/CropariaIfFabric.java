package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.network.CropSyncHandler;
import cool.muyucloud.croparia.registry.Recipes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;

public class CropariaIfFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CropariaIf.init();
        Recipes.forEach(typedSerializer -> RecipeSynchronization.synchronizeRecipeSerializer(typedSerializer.getSerializer()));
        PayloadTypeRegistry.serverboundConfiguration().register(CropSyncHandler.TaskAck.TYPE, CropSyncHandler.TaskAck.CODEC);
        PayloadTypeRegistry.clientboundConfiguration().registerLarge(CropSyncHandler.TYPE.type(), CropSyncHandler.CODEC, 16 * 1024 * 1024);
        ServerConfigurationNetworking.registerGlobalReceiver(
            CropSyncHandler.TaskAck.TYPE,
            (_, context) -> context.server().execute(() ->
                context.packetListener().completeTask(
                    CropSyncHandler.Task.TYPE
                )
            )
        );
    }
}
