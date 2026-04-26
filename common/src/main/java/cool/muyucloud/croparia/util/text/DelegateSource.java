package cool.muyucloud.croparia.util.text;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class DelegateSource<S> implements FailureMessenger, SuccessMessenger {
    public static final SimpleCommandExceptionType ERROR_NO_LEVEL = new SimpleCommandExceptionType(Texts.translatable("chat.croparia.error.no_level"));
    private static final Map<Class<?>, SourceFactory<?>> DELEGATE_MAP = new HashMap<>();

    public static <S> void register(Class<S> clz, SourceFactory<S> factory) {
        DELEGATE_MAP.put(clz, factory);
    }

    public static <S> DelegateSource<S> of(CommandContext<S> context) {
        S source = context.getSource();
        SourceFactory<?> raw = DELEGATE_MAP.get(source.getClass());
        if (raw == null) throw new IllegalArgumentException("No suitable delegation for class %s".formatted(source.getClass()));
        SourceFactory<S> factory = raw.cast();
        return factory.build(source);
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

    public abstract Optional<Level> getLevel();

    public Player getPlayerOrException() throws CommandSyntaxException {
        return this.getPlayer().orElseThrow(CommandSourceStack.ERROR_NOT_PLAYER::create);
    }

    public Level getLevelOrException() throws CommandSyntaxException {
        return this.getLevel().orElseThrow(ERROR_NO_LEVEL::create);
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
            Entity entity = this.getSource().getEntity();
            return entity instanceof Player player ? Optional.of(player) : Optional.empty();
        }

        @Override
        public Optional<Level> getLevel() {
            return Optional.of(this.getSource().getLevel());
        }
    }

    public interface SourceFactory<S> {
        DelegateSource<S> build(S source);

        @SuppressWarnings("unchecked")
        default <T> SourceFactory<T> cast() {
            return (SourceFactory<T>) this;
        }
    }
}
