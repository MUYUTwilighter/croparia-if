package cool.muyucloud.croparia.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;

public class ClientCommandRoot {
    public static final LiteralArgumentBuilder<Object> ROOT = LiteralArgumentBuilder.literal("croparia");

    public static void register() {
        CropariaIf.LOGGER.debug("Client commands are disabled on 1.20.1 until platform hooks are restored");
    }
}
