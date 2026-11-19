package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.config.Config;
import cool.muyucloud.croparia.config.ConfigFileHandler;
import cool.muyucloud.croparia.util.text.DelegateSource;
import cool.muyucloud.croparia.util.text.Texts;

public class ConfigCommand {
    public static <S> LiteralArgumentBuilder<S> buildFilePath() {
        return LiteralArgumentBuilder.<S>literal("filePath").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.filePath", Config.resolvePath(CropariaIf.CONFIG.getFilePath())), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, String>argument("value", StringArgumentType.greedyString()).executes(context -> {
            String path = StringArgumentType.getString(context, "value");
            CropariaIf.CONFIG.setFilePath(Config.parsePath(path).orElseThrow());
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.filePath", path), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildRecipeWizard() {
        return LiteralArgumentBuilder.<S>literal("recipeWizard").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.recipeWizard", Config.resolvePath(CropariaIf.CONFIG.getRecipeWizard())), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, String>argument("value", StringArgumentType.greedyString()).executes(context -> {
            String path = StringArgumentType.getString(context, "value");
            CropariaIf.CONFIG.setRecipeWizard(Config.parsePath(path).orElseThrow());
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.recipeWizard", path), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildInfusor() {
        return LiteralArgumentBuilder.<S>literal("infusor").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.infusor", CropariaIf.CONFIG.getInfusor().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Boolean>argument("value", BoolArgumentType.bool()).executes(context -> {
            CropariaIf.CONFIG.setInfusor(BoolArgumentType.getBool(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.infusor", CropariaIf.CONFIG.getInfusor().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildRitual() {
        return LiteralArgumentBuilder.<S>literal("ritual").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.ritual", CropariaIf.CONFIG.getRitual().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Boolean>argument("value", BoolArgumentType.bool()).executes(context -> {
            CropariaIf.CONFIG.setRitual(BoolArgumentType.getBool(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.ritual", CropariaIf.CONFIG.getRitual().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildFruitUse() {
        return LiteralArgumentBuilder.<S>literal("fruitUse").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.fruitUse", CropariaIf.CONFIG.getFruitUse().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Boolean>argument("value", BoolArgumentType.bool()).executes(context -> {
            CropariaIf.CONFIG.setFruitUse(BoolArgumentType.getBool(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.fruitUse", CropariaIf.CONFIG.getFruitUse().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildSoakAttempts() {
        return LiteralArgumentBuilder.<S>literal("soakAttempts").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.soakAttempts", CropariaIf.CONFIG.getSoakAttempts().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Integer>argument("value", IntegerArgumentType.integer()).suggests((context, builder) -> {
            builder.suggest(2);
            builder.suggest(0);
            return builder.buildFuture();
        }).executes(context -> {
            CropariaIf.CONFIG.setSoakAttempts(IntegerArgumentType.getInteger(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.soakAttempts", CropariaIf.CONFIG.getSoakAttempts().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildAutoReload() {
        return LiteralArgumentBuilder.<S>literal("autoReload").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.autoReload", CropariaIf.CONFIG.getAutoReload().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Boolean>argument("value", BoolArgumentType.bool()).executes(context -> {
            CropariaIf.CONFIG.setAutoReload(BoolArgumentType.getBool(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.autoReload", CropariaIf.CONFIG.getAutoReload().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildOverride() {
        return LiteralArgumentBuilder.<S>literal("override").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.override", CropariaIf.CONFIG.getOverride().toString()), false);
            return 1;
        }).then(RequiredArgumentBuilder.<S, Boolean>argument("value", BoolArgumentType.bool()).executes(context -> {
            CropariaIf.CONFIG.setOverride(BoolArgumentType.getBool(context, "value"));
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.override", CropariaIf.CONFIG.getOverride().toString()), false);
            return 1;
        }));
    }

    public static <S> LiteralArgumentBuilder<S> buildReset(boolean client) {
        return LiteralArgumentBuilder.<S>literal("reset").executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.reset.warn")
                .append(Texts.translatable("commands.croparia.config.reset.confirm"))
                .withStyle(Texts.runCommand(CommonCommandRoot.commandRoot(client), "reset", "confirm"))
                .withStyle(Texts.inlineMouseBehavior()), false);
            return 1;
        }).then(LiteralArgumentBuilder.<S>literal("confirm").executes(context -> {
            ConfigFileHandler.save(new Config());
            ConfigFileHandler.reload(CropariaIf.CONFIG);
            DelegateSource<S> source = DelegateSource.of(context);
            source.success(Texts.translatable("commands.croparia.config.reset.success"), false);
            return 1;
        }));
    }
}
