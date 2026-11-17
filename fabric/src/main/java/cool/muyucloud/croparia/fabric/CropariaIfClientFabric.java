package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.asset.CropItemSelectProperty;
import cool.muyucloud.croparia.api.crop.asset.CropItemTintSource;
import cool.muyucloud.croparia.api.crop.network.CropSyncHandler;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.client.CropariaIfClient;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.text.Texts;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

public class CropariaIfClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CropariaIfClient.init();
        CropariaIfClient.registerBlockTintSources(BlockColorRegistry::register);
        ClientRecipeSynchronizedEvent.EVENT.register((_, recipes) -> {
            Recipes.forEach(serializer -> receiveRecipes(recipes, serializer));
        });
        ItemTintSources.ID_MAPPER.put(CropariaIf.of("crop"), CropItemTintSource.CODEC);
        SelectItemModelProperties.ID_MAPPER.put(CropariaIf.of("crop_type"), CropItemSelectProperty.TYPE);
        ClientConfigurationNetworking.registerGlobalReceiver(CropSyncHandler.TYPE.type(), (handler, context) -> context.client().submit(() -> {
            handler.apply();
            context.responseSender().sendPacket(CropSyncHandler.TaskAck.INSTANCE);
        }).exceptionally(e -> {
            CropariaIf.LOGGER.error("Failed to apply crop definitions", e);
            context.responseSender().disconnect(
                Texts.literal("Failed to apply crop definitions")
            );
            return null;
        }));
    }

    @SuppressWarnings("unchecked")
    private static <I extends RecipeInput, R extends DisplayableRecipe<I>> void receiveRecipes(SynchronizedRecipes recipes, TypedSerializer<R> serializer) {
        serializer.syncRecipes(recipes.getAllOfType((RecipeType<R>) serializer.getRecipeType()).stream().toList());
    }
}
