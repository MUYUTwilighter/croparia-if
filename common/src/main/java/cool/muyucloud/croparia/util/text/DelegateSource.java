package cool.muyucloud.croparia.util.text;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class DelegateSource<S> implements FailureMessenger, SuccessMessenger {
    @SuppressWarnings("unchecked")
    public static <S> DelegateSource<S> of(CommandContext<S> context) {
        S source = context.getSource();
        if (source instanceof CommandSourceStack commonSource) {
            return (DelegateSource<S>) new CommonDelegateSource<>(commonSource);
        } else if (source instanceof ClientCommandRegistrationEvent.ClientCommandSourceStack clientSource) {
            return (DelegateSource<S>) new ClientDelegateSource<>(clientSource);
        } else {
            throw new UnsupportedOperationException("Unsupported command source type: " + source.getClass());
        }
    }

    @NotNull
    private final S source;

    public DelegateSource(@NotNull S source) {
        this.source = source;
    }

    public @NotNull S getSource() {
        return source;
    }

    public abstract Optional<Player> getPlayer();

    public abstract Level getLevel();

    public Player getPlayerOrException() throws CommandSyntaxException {
        return this.getPlayer().orElseThrow(CommandSourceStack.ERROR_NOT_PLAYER::create);
    }

    public static class CommonDelegateSource<S extends CommandSourceStack> extends DelegateSource<S> {
        public CommonDelegateSource(S source) {
            super(source);
        }

        @Override
        public void failure(Component msg) {
            this.getSource().sendFailure(msg);
        }

        @Override
        public void success(Component msg, boolean broadcast) {
            this.getSource().sendSuccess(() -> msg, broadcast);
        }

        @Override
        public Optional<Player> getPlayer() {
            return Optional.ofNullable(this.getSource().getPlayer());
        }

        @Override
        public Level getLevel() {
            return this.getSource().getLevel();
        }
    }

    public static class ClientDelegateSource<S extends ClientCommandRegistrationEvent.ClientCommandSourceStack> extends DelegateSource<S> {
        public ClientDelegateSource(S source) {
            super(source);
        }

        @Override
        public void failure(Component msg) {
            this.getSource().arch$sendFailure(msg);
        }

        @Override
        public void success(Component msg, boolean broadcast) {
            this.getSource().arch$sendSuccess(() -> msg, broadcast);
        }

        @Override
        public Optional<Player> getPlayer() {
            return Optional.ofNullable(this.getSource().arch$getPlayer());
        }

        @Override
        public Level getLevel() {
            return this.getSource().arch$getLevel();
        }
    }
}
