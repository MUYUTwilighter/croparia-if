package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.api.recipe.sync.SyncedRecipeCache;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getRecipeManager()Lnet/minecraft/world/item/crafting/RecipeManager;",
            shift = At.Shift.BEFORE
        )
    )
    private void syncCropariaRecipesBeforeVanillaRecipePackets(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        SyncedRecipeCache.syncPlayer(player);
    }

    @Inject(
        method = "reloadResources",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void syncCropariaRecipesBeforeReloadRecipePackets(CallbackInfo ci) {
        SyncedRecipeCache.syncAll(server);
    }
}
