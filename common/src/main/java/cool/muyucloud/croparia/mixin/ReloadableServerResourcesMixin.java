package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.pack.DataPackHandler;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @Inject(method = "loadResources", at = @At("HEAD"))
    private static void onDataReload(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> contextLayers,
                                     List<Registry.PendingTags<?>> updatedContextTags, FeatureFlagSet enabledFeatures,
                                     Commands.CommandSelection commandSelection, PermissionSet functionCompilationPermissions,
                                     Executor backgroundExecutor, Executor mainThreadExecutor,
                                     CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
        // Definition changes are owned by the sync command; /reload only rebuilds generated data.
        DataGenerator.LOGGER.debug("=== Data Pack Reload Triggered ===");
        DataPackHandler.REGISTRY.values().forEach(DataPackHandler::onTriggered);
    }

    @Inject(method = "loadResources", at = @At("RETURN"))
    private static void onDataLoaded(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> contextLayers,
                                     List<Registry.PendingTags<?>> updatedContextTags, FeatureFlagSet enabledFeatures,
                                     Commands.CommandSelection commandSelection, PermissionSet functionCompilationPermissions,
                                     Executor backgroundExecutor, Executor mainThreadExecutor,
                                     CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
        cir.getReturnValue().whenComplete((_, throwable) -> {
            if (throwable == null) {
                OnLoadSupplier.LAST_DATA_LOAD = System.currentTimeMillis();
            }
        });
    }
}
