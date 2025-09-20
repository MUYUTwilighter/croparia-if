package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.config.Config;
import cool.muyucloud.croparia.config.ConfigFileHandler;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ConfigCommand {
    private static final LiteralArgumentBuilder<CommandSourceStack> FILE_PATH = Commands.literal("filePath").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.filePath", Config.resolvePath(CropariaIf.CONFIG.getFilePath())), true);
        return 1;
    }).then(Commands.argument("value", StringArgumentType.greedyString()).executes(context -> {
        String path = StringArgumentType.getString(context, "value");
        CropariaIf.CONFIG.setFilePath(Config.parsePath(path).orElseThrow());
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.filePath", path), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> RECIPE_WIZARD = Commands.literal("recipeWizard").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.recipeWizard", Config.resolvePath(CropariaIf.CONFIG.getRecipeWizard()), true));
        return 1;
    }).then(Commands.argument("value", StringArgumentType.greedyString()).executes(context -> {
        String path = StringArgumentType.getString(context, "value");
        CropariaIf.CONFIG.setRecipeWizard(Config.parsePath(path).orElseThrow());
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.recipeWizard", path), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> INFUSOR = Commands.literal("infusor").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.infusor", CropariaIf.CONFIG.getInfusor().toString()), true);
        return 1;
    }).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
        CropariaIf.CONFIG.setInfusor(BoolArgumentType.getBool(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.infusor", CropariaIf.CONFIG.getInfusor().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> RITUAL = Commands.literal("ritual").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.ritual", CropariaIf.CONFIG.getRitual().toString()), true);
        return 1;
    }).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
        CropariaIf.CONFIG.setRitual(BoolArgumentType.getBool(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.ritual", CropariaIf.CONFIG.getRitual().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> FRUIT_USE = Commands.literal("fruitUse").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.fruitUse", CropariaIf.CONFIG.getFruitUse().toString()), true);
        return 1;
    }).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
        CropariaIf.CONFIG.setFruitUse(BoolArgumentType.getBool(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.fruitUse", CropariaIf.CONFIG.getFruitUse().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> SOAK_ATTEMPTS = Commands.literal("soakAttempts").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.soakAttempts", CropariaIf.CONFIG.getSoakAttempts().toString()), true);
        return 1;
    }).then(Commands.argument("value", IntegerArgumentType.integer()).suggests((context, builder) -> {
        builder.suggest("2");
        builder.suggest("0");
        return builder.buildFuture();
    }).executes(context -> {
        CropariaIf.CONFIG.setSoakAttempts(IntegerArgumentType.getInteger(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.soakAttempts", CropariaIf.CONFIG.getSoakAttempts().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> AUTO_RELOAD = Commands.literal("autoReload").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.autoReload", CropariaIf.CONFIG.getAutoReload().toString()), true);
        return 1;
    }).then(Commands.argument("value", IntegerArgumentType.integer()).suggests((context, builder) -> {
        builder.suggest("-1");
        builder.suggest("20");
        return builder.buildFuture();
    }).executes(context -> {
        CropariaIf.CONFIG.setAutoReload(IntegerArgumentType.getInteger(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.autoReload", CropariaIf.CONFIG.getAutoReload().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> OVERRIDE = Commands.literal("override").executes(context -> {
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.override", CropariaIf.CONFIG.getOverride().toString()), true);
        return 1;
    }).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
        CropariaIf.CONFIG.setOverride(BoolArgumentType.getBool(context, "value"));
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.override", CropariaIf.CONFIG.getOverride().toString()), true);
        return 1;
    }));
    private static final LiteralArgumentBuilder<CommandSourceStack> RESET = Commands.literal("reset").executes(context -> {
        Texts.chat(context.getSource(), Texts.translatable(
            "commands.croparia.config.reset.warn"
        ).append(Texts.translatable("commands.croparia.config.reset.confirm")
            .withStyle(Texts.runCommand("cropariaServer reset confirm"))
            .withStyle(Texts.inlineMouseBehavior())));
        return 1;
    }).then(Commands.literal("confirm").executes(context -> {
        ConfigFileHandler.save(new Config());
        ConfigFileHandler.reload(CropariaIf.CONFIG);
        Texts.success(context.getSource(), Texts.translatable("commands.croparia.config.reset.success"), true);
        return 1;
    }));

    public static ArgumentBuilder<CommandSourceStack, ?> buildFilePath() {
        return FILE_PATH;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildRecipeWizard() {
        return RECIPE_WIZARD;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildSoakAttempts() {
        return SOAK_ATTEMPTS;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildInfusor() {
        return INFUSOR;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildRitual() {
        return RITUAL;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildFruitUse() {
        return FRUIT_USE;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildAutoReload() {
        return AUTO_RELOAD;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildOverride() {
        return OVERRIDE;
    }

    public static ArgumentBuilder<CommandSourceStack, ?> buildReset() {
        return RESET;
    }
}
