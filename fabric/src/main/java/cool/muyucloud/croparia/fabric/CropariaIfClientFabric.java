package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.client.CropariaIfClient;
import cool.muyucloud.croparia.registry.Recipes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Collection;

public class CropariaIfClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CropariaIfClient.init();
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> {
            Recipes.forEach(serializer -> receiveRecipes(recipes, serializer));
        });
    }

    @SuppressWarnings("unchecked")
    private static <I extends RecipeInput, R extends DisplayableRecipe<I>> void receiveRecipes(SynchronizedRecipes recipes, TypedSerializer<R> serializer) {
        serializer.syncRecipes(recipes.getAllOfType((RecipeType<R>) serializer.getRecipeType()).stream().toList());
    }
}
