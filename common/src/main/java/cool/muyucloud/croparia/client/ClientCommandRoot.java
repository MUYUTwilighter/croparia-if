package cool.muyucloud.croparia.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.croparia.api.core.command.CropCommand;
import cool.muyucloud.croparia.api.core.command.GeneratorCommand;

public class ClientCommandRoot {
    public static <S> LiteralArgumentBuilder<S> build() {
        LiteralArgumentBuilder<S> root = LiteralArgumentBuilder.literal("croparia");
        return root.then(CropCommand.buildCrop(true))
            .then(CropCommand.buildCrop(true))
            .then(GeneratorCommand.buildGenerator(true));
    }
}
