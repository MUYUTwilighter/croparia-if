package cool.muyucloud.croparia.forge;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.client.ClientCommandRoot;
import cool.muyucloud.croparia.util.text.DelegateSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandSourceStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = CropariaIf.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CropariaIfForgeBus {
    static {
        DelegateSource.register(ClientCommandSourceStack.class, source -> new DelegateSource<>(source) {
            @Override
            public Optional<Player> getPlayer() {
                Entity entity = source.getEntity();
                return entity instanceof Player player ? Optional.of(player) : Optional.empty();
            }

            @Override
            public Optional<Level> getLevel() {
                return Optional.ofNullable(getSource().getUnsidedLevel());
            }

            @Override
            public void failure(Component msg) {
                this.getSource().sendFailure(msg);
            }

            @Override
            public void success(Component msg, boolean broadcast) {
                this.getSource().sendSuccess(() -> msg, broadcast);
            }
        });
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(ClientCommandRoot.build());
    }
}
