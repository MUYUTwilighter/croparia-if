package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.network.CropSyncHandler;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CropariaIf.MOD_ID)
@EventBusSubscriber(modid = CropariaIf.MOD_ID)
public class CropariaIfNeoForge {
    public CropariaIfNeoForge() {
        CropariaIf.init();
    }

    @SubscribeEvent
    public static void onRecipeSend(OnDatapackSyncEvent event) {
        Recipes.forEach(typedSerializer -> event.sendRecipes(typedSerializer.getRecipeType()));
        event.sendRecipes();
    }

    @SubscribeEvent
    public static void onRecipeReceived(RecipesReceivedEvent event) {
        Recipes.forEach(typedSerializer -> receiveRecipe(event.getRecipeMap(), typedSerializer.adapt()));
    }

    @SubscribeEvent
    public static void onRegisterPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").configurationToClient(
            CropSyncHandler.TYPE.type(), CropSyncHandler.CODEC,
            (payload, context) -> context.enqueueWork(() -> {
                payload.apply();
                context.reply(CropSyncHandler.TaskAck.INSTANCE);
            }).exceptionally(error -> {
                CropariaIf.LOGGER.error("Failed to apply crop definitions", error);
                context.disconnect(
                    Texts.literal("Failed to apply crop definitions")
                );
                // A Void future recovers with null after the failed connection is closed.
                //noinspection DataFlowIssue
                return null;
            })
        ).configurationToServer(
            CropSyncHandler.TaskAck.TYPE, CropSyncHandler.TaskAck.CODEC,
            (_, context) -> context.enqueueWork(() ->
                context.finishCurrentTask(CropSyncHandler.Task.TYPE)
            )
        );
    }

    private static <I extends RecipeInput, R extends DisplayableRecipe<I>> void receiveRecipe(RecipeMap map, TypedSerializer<R> typedSerializer) {
        @SuppressWarnings("unchecked")
        RecipeType<R> type = (RecipeType<R>) typedSerializer.getRecipeType();
        typedSerializer.syncRecipes(map.byType(type));
    }
}
