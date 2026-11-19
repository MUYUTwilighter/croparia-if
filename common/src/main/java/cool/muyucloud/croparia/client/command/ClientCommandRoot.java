package cool.muyucloud.croparia.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.command.ConfigCommand;
import cool.muyucloud.croparia.api.core.command.CropCommand;
import cool.muyucloud.croparia.api.core.command.GeneratorCommand;
import cool.muyucloud.croparia.api.core.command.SyncCommand;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;

public class ClientCommandRoot {
    public static final LiteralArgumentBuilder<ClientCommandRegistrationEvent.ClientCommandSourceStack> ROOT =
        LiteralArgumentBuilder.literal("croparia");

    public static void register() {
        CropariaIf.LOGGER.debug("Registering client commands");
        ROOT.then(new SyncCommand<ClientCommandRegistrationEvent.ClientCommandSourceStack>(true).build())
            .then(CropCommand.buildCrop(true))
            .then(CropCommand.buildMelon(true))
            .then(GeneratorCommand.buildGenerator(true))
            .then(ConfigCommand.buildFilePath())
            .then(ConfigCommand.buildRecipeWizard())
            .then(ConfigCommand.buildInfusor())
            .then(ConfigCommand.buildRitual())
            .then(ConfigCommand.buildFruitUse())
            .then(ConfigCommand.buildAutoReload())
            .then(ConfigCommand.buildOverride())
            .then(ConfigCommand.buildSoakAttempts())
            .then(ConfigCommand.buildReset(true));
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> dispatcher.register(ROOT));
    }
}
