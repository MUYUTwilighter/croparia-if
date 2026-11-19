package cool.muyucloud.croparia.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.command.ConfigCommand;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ConfigCommandTest {
    @AfterEach
    void restoreGameFolder() {
        ConfigFileHandler.resetGameFolderSupplierForTest();
    }

    @Test
    void clientCanQueryAndChangeAutoReload(@TempDir Path tempDir) throws CommandSyntaxException {
        ConfigFileHandler.setGameFolderSupplierForTest(() -> tempDir);
        boolean previous = CropariaIf.CONFIG.getAutoReload();
        List<Component> feedback = new ArrayList<>();
        ClientCommandSourceStack source = (ClientCommandSourceStack) Proxy.newProxyInstance(
            ClientCommandSourceStack.class.getClassLoader(), new Class<?>[]{ClientCommandSourceStack.class},
            (_, method, args) -> {
                if (method.getName().equals("arch$sendSuccess")) {
                    feedback.add((Component) ((Supplier<?>) args[0]).get());
                    return null;
                }
                throw new AssertionError("Unexpected source call: " + method.getName());
            });
        CommandDispatcher<ClientCommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<ClientCommandSourceStack>literal("croparia")
            .then(ConfigCommand.buildAutoReload()));
        try {
            assertEquals(1, dispatcher.execute("croparia autoReload true", source));
            assertTrue(CropariaIf.CONFIG.getAutoReload());
            assertEquals(1, dispatcher.execute("croparia autoReload", source));
            assertEquals(1, dispatcher.execute("croparia autoReload false", source));
            assertFalse(CropariaIf.CONFIG.getAutoReload());
            assertEquals(3, feedback.size());
            assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("croparia autoReload 1", source));
            assertFalse(CropariaIf.CONFIG.getAutoReload());
        } finally {
            CropariaIf.CONFIG.setAutoReload(previous);
        }
    }
}
