package cool.muyucloud.croparia.neoforge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.registry.Recipes;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@Mod(CropariaIf.MOD_ID)
public class CropariaIfNeoForge {
    public CropariaIfNeoForge() {
        CropariaIf.init();
    }

    @SubscribeEvent
    public void onRecipeSend(OnDatapackSyncEvent event) {
        Recipes.forEach(typedSerializer -> {
            // TODO: need predication to every single recipe holder
            // typedSerializer.findHolders().stream().filter(typedSerializer::shouldSync);
            event.sendRecipes(typedSerializer.getRecipeType());
        });
        event.sendRecipes();
    }

    @SubscribeEvent
    public void onRecipeReceived(RecipesReceivedEvent event) {
        Recipes.forEach(typedSerializer -> receiveRecipe(event.getRecipeMap(), typedSerializer));

    }

    private static <I extends RecipeInput, R extends DisplayableRecipe<I>> void receiveRecipe(RecipeMap map, TypedSerializer<R> typedSerializer) {
        @SuppressWarnings("unchecked")
        RecipeType<R> type = (RecipeType<R>) typedSerializer.getRecipeType();
        typedSerializer.syncRecipes(map.byType(type));
    }
}
