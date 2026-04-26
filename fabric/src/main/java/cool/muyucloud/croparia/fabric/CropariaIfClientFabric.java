package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.client.ClientCommandRoot;
import cool.muyucloud.croparia.client.CropariaIfClient;
import cool.muyucloud.croparia.util.text.DelegateSource;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class CropariaIfClientFabric implements ClientModInitializer {
    static {
        DelegateSource.register(FabricClientCommandSource.class, source -> new DelegateSource<>(source) {
            @Override
            public Optional<Player> getPlayer() {
                return Optional.ofNullable(source.getPlayer());
            }

            @Override
            public Optional<Level> getLevel() {
                return Optional.ofNullable(source.getWorld());
            }

            @Override
            public void failure(Component msg) {
                source.sendError(msg);
            }

            @Override
            public void success(Component msg, boolean broadcast) {
                source.sendFeedback(msg);
            }
        });
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandRoot.build()));
        CropariaIfClient.init();
    }
}
