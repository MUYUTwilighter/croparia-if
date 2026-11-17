package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.registry.DgRegistries;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {
    @Inject(method = "load", at = @At("HEAD"))
    private static <D, R> void onLoad(WorldLoader.InitConfig config, WorldLoader.WorldDataSupplier<D> worldDataSupplier,
                                      WorldLoader.ResultFactory<D, R> resultFactory, Executor backgroundExecutor,
                                      Executor mainThreadExecutor, CallbackInfoReturnable<CompletableFuture<R>> cir) {
        DgRegistries.CROPS.loadCrops();
        DgRegistries.MELONS.loadCrops();
    }
}
