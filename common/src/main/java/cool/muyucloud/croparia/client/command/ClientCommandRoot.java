package cool.muyucloud.croparia.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.command.CropCommand;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;

public class ClientCommandRoot {
    public static final LiteralArgumentBuilder<ClientCommandRegistrationEvent.ClientCommandSourceStack> ROOT =
        LiteralArgumentBuilder.literal("croparia");

    public static void register() {
        CropariaIf.LOGGER.debug("Registering client commands");
        ROOT.then(CropCommand.buildCrop(true))
            .then(CropCommand.buildMelon(true));
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> dispatcher.register(ROOT));
    }
}
