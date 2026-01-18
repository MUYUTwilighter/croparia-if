package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.DataGenerator;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.JarJarEntry;
import cool.muyucloud.croparia.util.ResourceLocationArgument;
import cool.muyucloud.croparia.util.text.DelegateSource;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneratorCommand {
    public static <S> LiteralArgumentBuilder<S> buildGenerator(boolean client) {
        LiteralArgumentBuilder<S> generator = LiteralArgumentBuilder.literal("generator");
        LiteralArgumentBuilder<S> dumpBuiltin = buildDumpBuiltin(client);
        LiteralArgumentBuilder<S> query = buildQuery();
        generator.then(dumpBuiltin);
        generator.then(query);
        return generator;
    }

    public static <S> LiteralArgumentBuilder<S> buildDumpBuiltin(boolean client) {
        LiteralArgumentBuilder<S> dumpBuiltin = LiteralArgumentBuilder.literal("dumpBuiltin");
        RequiredArgumentBuilder<S, ResourceLocation> pack = buildPack();
        pack.executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            Optional<PackHandler> mayHandler = PackHandler.byId(packId);
            if (mayHandler.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badPack"));
                return -1;
            }
            Path packRoot = mayHandler.get().getRoot();
            String prefix = "data-generators/%s/%s/".formatted(packId.getNamespace(), packId.getNamespace());
            AtomicInteger success = new AtomicInteger();
            PackHandler.getBuiltinGenerators(packId).forEach(jarJarEntry -> {
                String generatorVal = jarJarEntry.getJarEntry().getName().substring(prefix.length());
                Path target = packRoot.resolve(generatorVal);
                try {
                    jarJarEntry.forInputStream(inputStream -> {
                        try (FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
                            inputStream.transferTo(outputStream);
                        }
                    });
                    success.incrementAndGet();
                } catch (IOException e) {
                    source.failure(Texts.translatable("commands.croparia.generator.dumpBuiltin.fail", generatorVal));
                    CropariaIf.LOGGER.error("Failed to dump generator %s from pack %s".formatted(generatorVal, packId.toString()), e);
                }
            });
            MutableComponent result = Texts.translatable("commands.croparia.generator.dumpBuiltin.success", success.get());
            if (client) {
                MutableComponent openOp = Texts.openFileButton(packRoot.toAbsolutePath().toString());
                result.append(openOp);
            }
            source.success(result, false);
            return success.get();
        });
        RequiredArgumentBuilder<S, String> generator = RequiredArgumentBuilder.argument("generator", StringArgumentType.greedyString());
        generator.suggests((context, builder) -> {
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            String prefix = "data-generators/%s/%s/".formatted(packId.getNamespace(), packId.getPath());
            PackHandler.getBuiltinGenerators(packId).forEach(jarJarEntry -> {
                String entryName = jarJarEntry.getJarEntry().getName().substring(prefix.length());
                builder.suggest(entryName);
            });
            return builder.buildFuture();
        }).executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            Optional<PackHandler> mayHandler = PackHandler.byId(packId);
            if (mayHandler.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badPack"));
                return -1;
            }
            Path generatorRoot = mayHandler.get().getGeneratorRoot();
            String generatorVal = StringArgumentType.getString(context, "generator");
            String entryName = "data-generators/%s/%s/%s".formatted(packId.getNamespace(), packId.getPath(), generatorVal);
            for (JarJarEntry entry : PackHandler.getBuiltinGenerators(packId)) {
                if (entry.getJarEntry().getName().equals(entryName)) {
                    Path target = generatorRoot.resolve(generatorVal);
                    try {
                        entry.forInputStream(inputStream -> {
                            try (FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
                                inputStream.transferTo(outputStream);
                            }
                        });
                        MutableComponent result = Texts.translatable("commands.croparia.generator.dumpBuiltin.success", 1);
                        if (client) {
                            MutableComponent openOp = Texts.openFileButton(target.toAbsolutePath().toString());
                            result.append(openOp);
                        }
                        source.success(result, false);
                        return 1;
                    } catch (IOException e) {
                        MutableComponent io = Texts.translatable("commands.croparia.generator.dumpBuiltin.fail", generatorVal);
                        source.failure(io);
                        return -1;
                    }
                }
            }
            MutableComponent notFound = Texts.translatable("commands.croparia.generator.badGenerator");
            source.failure(notFound);
            return -1;
        });
        return dumpBuiltin.then(pack.then(generator));
    }

    public static <S> LiteralArgumentBuilder<S> buildQuery() {
        LiteralArgumentBuilder<S> query = LiteralArgumentBuilder.literal("query");
        RequiredArgumentBuilder<S, ResourceLocation> pack = buildPack();
        pack.executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            Optional<PackHandler> mayHandler = PackHandler.byId(packId);
            if (mayHandler.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badPack"));
                return -1;
            }
            PackHandler handler = mayHandler.get();
            MutableComponent result = Texts.translatable("commands.croparia.generator.query.pack",
                handler.getId().toString(),
                handler.getRoot().toString()
            );
            source.success(result, false);
            return 1;
        });
        RequiredArgumentBuilder<S, String> generator = RequiredArgumentBuilder.argument("generator", StringArgumentType.greedyString());
        generator.suggests((context, builder) -> {
            DelegateSource<S> source = DelegateSource.of(context);
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            Optional<PackHandler> mayHandler = PackHandler.byId(packId);
            if (mayHandler.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badPack"));
                return builder.buildFuture();
            }
            mayHandler.get().suggestGenerators(builder);
            return builder.buildFuture();
        }).executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            ResourceLocation packId = ResourceLocationArgument.getId(context, "pack");
            Optional<PackHandler> mayHandler = PackHandler.byId(packId);
            if (mayHandler.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badPack"));
                return -1;
            }
            String generatorVal = StringArgumentType.getString(context, "generator");
            Optional<DataGenerator> mayEntry = mayHandler.get().getGenerator(generatorVal);
            if (mayEntry.isEmpty()) {
                source.failure(Texts.translatable("commands.croparia.generator.badGenerator"));
                return -1;
            }
            DataGenerator entry = mayEntry.get();
            source.success(Texts.translatable(
                "commands.croparia.generator.query.entry",
                String.valueOf(entry.getName()),
                entry.getType().toString(),
                entry.getRegistry().getId().toString()
            ), false);
            return 1;
        });
        return query.then(pack.then(generator));
    }

    public static <S> RequiredArgumentBuilder<S, ResourceLocation> buildPack() {
        RequiredArgumentBuilder<S, ResourceLocation> pack = RequiredArgumentBuilder.argument("pack", ResourceLocationArgument.id());
        pack.suggests((context, builder) -> {
            PackHandler.suggestPacks(builder);
            return builder.buildFuture();
        });
        return pack;
    }
}
