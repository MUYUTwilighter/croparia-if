package cool.muyucloud.croparia.mixin;

import cool.muyucloud.croparia.api.crop.network.CropSyncHandler;
import cool.muyucloud.croparia.registry.DgRegistries;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {
    @Shadow
    @Final
    private Queue<ConfigurationTask> configurationTasks;

    @Inject(method = "addOptionalTasks", at = @At("HEAD"))
    public void cif$addCropSyncTask(CallbackInfo ci) {
        configurationTasks.add(new CropSyncHandler.Task(false, DgRegistries.CROPS, DgRegistries.MELONS));
    }
}
