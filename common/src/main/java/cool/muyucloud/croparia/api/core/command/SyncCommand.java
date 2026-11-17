package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.network.CropSyncHandler;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.mixin.CreativeModeTabsAccessor;
import cool.muyucloud.croparia.util.IdentifierArgument;
import cool.muyucloud.croparia.util.text.DelegateSource;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SyncCommand<S> {
    private final LiteralArgumentBuilder<S> sync = LiteralArgumentBuilder.literal("sync");

    public SyncCommand(boolean client) {
        List<CropRegistry<?>> registries = new ArrayList<>();
        for (DgRegistry<?> reg : DgRegistry.REGISTRY.values()) {
            if (reg instanceof CropRegistry<?> cropReg) {
                registries.add(cropReg);
            }
        }
        RequiredArgumentBuilder<S, Identifier> perReg = RequiredArgumentBuilder.argument("registry", IdentifierArgument.id());
        LiteralArgumentBuilder<S> perRegNoReload = LiteralArgumentBuilder.literal("noReload");
        LiteralArgumentBuilder<S> all = LiteralArgumentBuilder.literal("all");
        LiteralArgumentBuilder<S> allNoReload = LiteralArgumentBuilder.literal("noReload");

        sync.executes(context -> execute(context, client, true, registries));
        perReg.suggests((_, builder) -> {
            registries.forEach(registry -> builder.suggest(registry.getId().toString()));
            return builder.buildFuture();
        }).executes(context -> {
            Identifier regId = IdentifierArgument.getId(context, "registry");
            return execute(context, client, true, regId);
        });
        perRegNoReload.executes(context -> {
            Identifier regId = IdentifierArgument.getId(context, "registry");
            return execute(context, client, false, regId);
        });
        all.executes(context -> execute(context, client, true, registries));
        allNoReload.executes(context -> execute(context, client, false, registries));

        perReg.then(perRegNoReload);
        all.then(allNoReload);
        sync.then(perReg).then(all);
    }

    public LiteralArgumentBuilder<S> build() {
        return sync;
    }

    private static <S> int execute(CommandContext<S> context, boolean client, boolean reload, Identifier regId) {
        DelegateSource<S> source = DelegateSource.of(context);
        if (regId == null) return 0;
        DgRegistry<?> dgReg = DgRegistry.REGISTRY.get(regId);
        if (!(dgReg instanceof CropRegistry<?> cropReg)) {
            source.failure(Texts.translatable("commands.croparia.sync.unknown_registry", regId.toString()));
            return 0;
        }
        return execute(context, client, reload, List.of(cropReg));
    }

    private static <S> int execute(CommandContext<S> context, boolean client, boolean reload, List<CropRegistry<?>> registries) {
        DelegateSource<S> source = DelegateSource.of(context);
        CropariaIf.ifServerOrElse(server -> server.execute(() -> {
            source.success(Texts.translatable("commands.croparia.sync.reload_requested"), true);
            for (CropRegistry<?> registry : registries) registry.loadCrops();
            new CropSyncHandler(reload, registries).send(server.getPlayerList().getPlayers().stream()
                .filter(player -> !client || !server.isSingleplayerOwner(player.nameAndId())).toList());
            if (reload) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            }
            if (client) CropariaIf.ifClient(ref -> ref.get().execute(() -> {
                CreativeModeTabsAccessor.cif$setCachedParams(null);
                if (reload) ref.get().reloadResourcePacks();
            }));
        }), () -> {
            source.success(Texts.translatable("commands.croparia.sync.reload_requested"), false);
            if (reload && client) {
                CropariaIf.ifClient(ref -> ref.get().reloadResourcePacks());
            }
        });
        return 1;
    }
}
