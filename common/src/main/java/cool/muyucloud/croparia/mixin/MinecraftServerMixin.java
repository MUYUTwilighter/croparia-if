package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow
    @Nullable
    public abstract ServerLevel getLevel(ResourceKey<Level> dimension);

    @Inject(method = "reloadResources", at = @At("RETURN"))
    public void onReloaded(Collection<String> selectedIds, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().whenComplete((v, t) -> OnLoadSupplier.LAST_DATA_LOAD = System.currentTimeMillis());
    }
}
