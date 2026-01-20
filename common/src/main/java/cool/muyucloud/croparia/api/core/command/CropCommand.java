package cool.muyucloud.croparia.api.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.*;
import cool.muyucloud.croparia.api.crop.block.CropariaCropBlock;
import cool.muyucloud.croparia.api.crop.block.MelonStem;
import cool.muyucloud.croparia.api.crop.item.Croparia;
import cool.muyucloud.croparia.api.crop.util.*;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.registry.DgRegistries;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.ResourceLocationArgument;
import cool.muyucloud.croparia.util.text.DelegateSource;
import cool.muyucloud.croparia.util.text.SuccessMessenger;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class CropCommand<C extends AbstractCrop<?>> {
    public static final CropCommand<Crop> CROP = new CropCommand<>("crop", DgRegistries.CROPS) {
        @Override
        public MutableComponent buildReport(Crop crop) {
            MutableComponent name = reportName(crop);
            MutableComponent translation = reportTranslation(crop);
            MutableComponent material = reportMaterial(crop);
            MutableComponent tier = reportTier(crop.getTier());
            MutableComponent color = reportColor(crop.getColor());
            MutableComponent type = Texts.translatable(
                "commands.croparia.crop.query.type", Texts.literal(crop.getType(), Texts.copyText(crop.getType()))
            );
            MutableComponent seed = reportSeed(crop.getSeedId());
            MutableComponent fruit = reportFruit(crop.getFruitId());

            MutableComponent cropBlock = Texts.translatable(
                "commands.croparia.crop.query.cropBlock",
                Texts.literal(crop.getBlockId().toString(),
                    Texts.suggestCommand("setblock ~ ~ ~", crop.getBlockId() + "[age=7]"),
                    Texts.hoverText(crop.getCropBlock().map(CropariaCropBlock::getName).orElse(Texts.literal("error"))),
                    Texts.inlineMouseBehavior())
            );
            MutableComponent status = diagnose(crop);
            return name.append("\n")
                .append(translation).append("\n")
                .append(material).append("\n")
                .append(tier).append("\n")
                .append(color).append("\n")
                .append(type).append("\n")
                .append(seed).append("\n")
                .append(fruit).append("\n")
                .append(cropBlock).append("\n")
                .append(status);
        }
    };
    public static final CropCommand<Melon> MELON = new CropCommand<>("melon", DgRegistries.MELONS) {
        @Override
        public MutableComponent buildReport(Melon melon) {
            MutableComponent name = CropCommand.reportName(melon);
            MutableComponent translation = CropCommand.reportTranslation(melon);
            MutableComponent material = CropCommand.reportMaterial(melon);
            MutableComponent tier = CropCommand.reportTier(melon.getTier());
            MutableComponent color = CropCommand.reportColor(melon.getColor());
            MutableComponent seed = CropCommand.reportSeed(melon.getSeed().getId());
            MutableComponent fruit = CropCommand.reportFruit(melon.getMelon().getId());
            MutableComponent stem = Texts.translatable(
                "commands.croparia.crop.query.cropBlock",
                Texts.literal(melon.getStem().getId().toString(),
                    Texts.suggestCommand("setblock ~ ~ ~", melon.getStem().getId() + "[age=7]"),
                    Texts.hoverText(melon.getStem().toOptional().map(MelonStem::getName).orElse(Texts.literal("error"))),
                    Texts.inlineMouseBehavior())
            );
            MutableComponent status = diagnose(melon);
            return name.append("\n")
                .append(translation).append("\n")
                .append(material).append("\n")
                .append(tier).append("\n")
                .append(color).append("\n")
                .append(seed).append("\n")
                .append(fruit).append("\n")
                .append(stem).append("\n")
                .append(status);
        }
    };

    public static <S> LiteralArgumentBuilder<S> buildCrop(boolean client) {
        LiteralArgumentBuilder<S> create = LiteralArgumentBuilder.literal("create");
        RequiredArgumentBuilder<S, ResourceLocation> id = RequiredArgumentBuilder.argument("id", ResourceLocationArgument.id());
        id.executes(context -> createCrop(context, client, false));
        RequiredArgumentBuilder<S, String> color = RequiredArgumentBuilder.argument("color", StringArgumentType.word());
        color.executes(context -> createCrop(context, client, false));
        RequiredArgumentBuilder<S, String> type = RequiredArgumentBuilder.argument("type", StringArgumentType.word());
        type.suggests((context, builder) -> {
            Crop.PRESET_TYPES.forEach(builder::suggest);
            return builder.buildFuture();
        }).executes(context -> createCrop(context, client, false));
        LiteralArgumentBuilder<S> force = LiteralArgumentBuilder.literal("force");
        force.executes(context -> createCrop(context, client, true));
        LiteralArgumentBuilder<S> crop = CROP.buildRoot();
        crop.then(CROP.buildDump(client));
        crop.then(CROP.buildQuery());
        crop.then(create.then(color.then(type.then(id.then(force)))));
        return crop;
    }

    public static <S> LiteralArgumentBuilder<S> buildMelon(boolean client) {
        LiteralArgumentBuilder<S> create = LiteralArgumentBuilder.literal("create");
        RequiredArgumentBuilder<S, ResourceLocation> id = RequiredArgumentBuilder.argument("id", ResourceLocationArgument.id());
        id.executes(context -> createMelon(context, client, false));
        RequiredArgumentBuilder<S, String> color = RequiredArgumentBuilder.argument("color", StringArgumentType.word());
        color.executes(context -> createMelon(context, client, false));
        LiteralArgumentBuilder<S> force = LiteralArgumentBuilder.literal("force");
        force.executes(context -> createMelon(context, client, true));
        LiteralArgumentBuilder<S> crop = MELON.buildRoot();
        crop.then(MELON.buildDump(client));
        crop.then(MELON.buildQuery());
        crop.then(create.then(color.then(id.then(force))));
        return crop;
    }

    private static @NotNull MutableComponent reportFruit(ResourceLocation fruit) {
        return Texts.translatable(
            "commands.croparia.crop.query.fruit",
            Texts.literal(fruit.toString(),
                Texts.suggestCommand("give @s", fruit.toString()),
                Texts.hoverItem(fruit),
                Texts.inlineMouseBehavior())
        );
    }

    private final String rootName;
    private final CropRegistry<C> registry;

    public CropCommand(String rootName, CropRegistry<C> registry) {
        this.rootName = rootName;
        this.registry = registry;
    }

    public <S> LiteralArgumentBuilder<S> buildRoot() {
        return LiteralArgumentBuilder.literal(this.getRootName());
    }

    public <S> LiteralArgumentBuilder<S> buildQuery() {
        LiteralArgumentBuilder<S> query = LiteralArgumentBuilder.literal("query");
        query.executes(context -> {
            DelegateSource<S> source = DelegateSource.of(context);
            return reportForPlayer(source);
        });
        RequiredArgumentBuilder<S, ResourceLocation> id = RequiredArgumentBuilder.argument("id", ResourceLocationArgument.id());
        id.suggests((context, builder) -> {
            this.getRegistry().forEach(crop -> builder.suggest(crop.getKey().toString()));
            return builder.buildFuture();
        });
        id.executes(context -> {
            ResourceLocation idVal = ResourceLocationArgument.getId(context, "id");
            return this.reportSingular(idVal, DelegateSource.of(context));
        });
        return query.then(id);
    }

    public <S> LiteralArgumentBuilder<S> buildDump(boolean openFile) {
        LiteralArgumentBuilder<S> dump = LiteralArgumentBuilder.literal("dump");
        dump.executes(context -> this.dumpAll(DelegateSource.of(context), openFile));
        RequiredArgumentBuilder<S, ResourceLocation> id = RequiredArgumentBuilder.argument("id", ResourceLocationArgument.id());
        id.suggests((context, builder) -> {
            this.getRegistry().forEach(crop -> builder.suggest(crop.getKey().toString()));
            return builder.buildFuture();
        });
        id.executes(context -> {
            ResourceLocation idVal = ResourceLocationArgument.getId(context, "id");
            return this.dump(idVal, DelegateSource.of(context), openFile);
        });
        return dump.then(id);
    }

    public String getRootName() {
        return rootName;
    }

    public CropRegistry<C> getRegistry() {
        return registry;
    }

    /* QUERIES */
    public <S> int reportSingular(ResourceLocation id, DelegateSource<S> source) {
        Optional<C> mayCrop = registry.forName(id);
        if (mayCrop.isEmpty()) {
            source.failure(Texts.translatable("commands.croparia.crop.query.absent", id));
            return 0;
        }
        C crop = mayCrop.get();
        MutableComponent report = buildReport(crop);
        source.success(report, false);
        return 1;
    }

    public <S> int reportForPlayer(DelegateSource<S> source) throws CommandSyntaxException {
        Player player = source.getPlayerOrException();
        Level world = source.getLevel();
        C crop = null;
        if (player.getWeaponItem().getItem() instanceof CropAccess<?> access) {
            crop = CropAccess.tryGet(access);
        } else if (world.getBlockState(CifUtil.lookingAt(player)).getBlock() instanceof CropAccess<?> access) {
            crop = CropAccess.tryGet(access);
        }
        if (crop == null) {
            source.failure(Texts.translatable("commands.croparia.crop.query.no_crop"));
            return 0;
        }
        MutableComponent report = buildReport(crop);
        source.success(report, false);
        return 1;
    }

    public abstract MutableComponent buildReport(C crop);

    private static @NotNull MutableComponent reportSeed(ResourceLocation seed) {
        return Texts.translatable(
            "commands.croparia.crop.query.seed",
            Texts.literal(seed.toString(),
                Texts.suggestCommand("give @s", seed.toString()),
                Texts.hoverItem(seed),
                Texts.inlineMouseBehavior())
        );
    }

    private static @NotNull MutableComponent reportColor(Color color) {
        return Texts.translatable(
            "commands.croparia.crop.query.color",
            Texts.literal(color.toString(), Texts.copyText(color.toString())).withColor(color.getValue())
        );
    }

    private static @NotNull MutableComponent reportTier(int tier) {
        return Texts.forStyles(Texts.translatable(
            "commands.croparia.crop.query.tier", Texts.literal(tier + "",
                Texts.suggestCommand("give @s", CropariaItems.getCroparia(tier).getId().toString()),
                Texts.hoverItem(CropariaItems.getCroparia(tier).get()),
                Texts.inlineMouseBehavior())
        ));
    }

    private static @NotNull <T> MutableComponent reportMaterial(AbstractCrop<T> crop) {
        Material<T> material = crop.getMaterial();
        MutableComponent name = Texts.literal(material.getName(), Texts.copyText(material.getName()), Texts.inlineMouseBehavior());
        if (material.isEmpty()) {
            return Texts.translatable("commands.croparia.crop.query.material", name);
        } else {
            ItemStack stack = material.asItem();
            name.withStyle(Texts.hoverItem(stack));
            name.withStyle(Texts.suggestCommand("give", "@s", Objects.requireNonNull(stack.getItem().arch$registryName()).toString()));
            return Texts.translatable("commands.croparia.crop.query.material", name);
        }
    }

    private static @NotNull MutableComponent reportTranslation(AbstractCrop<?> crop) {
        @Nullable
        String key = crop.getTranslationKey();
        MutableComponent keyComp;
        if (key == null) keyComp = Texts.literal("not loaded");
        else keyComp = Texts.forStyles(Texts.translatable(key),
            Texts.hoverText(key),
            Texts.copyText(key));
        return Texts.translatable("commands.croparia.crop.query.translationKey", keyComp);
    }

    private static @NotNull MutableComponent reportName(AbstractCrop<?> crop) {
        return Texts.translatable("commands.croparia.crop.query.id", crop.getKey().toString());
    }

    public static MutableComponent diagnose(@NotNull AbstractCrop<?> crop) {
        if (!crop.shouldLoad()) {
            return Texts.translatable("commands.croparia.crop.query.status.unavailable").withStyle(ChatFormatting.YELLOW);
        }
        if (crop.getMaterial().isEmpty()) {
            return Texts.translatable("commands.croparia.crop.query.status.material").withStyle(ChatFormatting.RED);
        }
        return Texts.translatable("commands.croparia.crop.query.status.good").withStyle(ChatFormatting.GREEN);
    }

    /* DUMP */
    public int dumpAll(SuccessMessenger success, boolean openFile) {
        int size = this.getRegistry().size();
        MutableComponent component = Texts.translatable("commands.croparia.crop.dump.perform", size);
        if (openFile) {
            MutableComponent openFileButton = Texts.openFileButton(
                this.getRegistry().getPath().toAbsolutePath().toString()
            );
            component.append(" ").append(openFileButton);
        }
        success.success(component, true);
        this.getRegistry().dumpCrops();
        return size;
    }

    public <S> int dump(ResourceLocation id, DelegateSource<S> messenger, boolean openFile) {
        Optional<C> optional = this.getRegistry().forName(id);
        if (optional.isEmpty()) {
            MutableComponent component = Texts.translatable("commands.croparia.crop.dump.singular.absent", id);
            messenger.failure(component);
            return 0;
        }
        Path dumped = this.getRegistry().dumpCrop(optional.get());
        if (dumped != null) {
            MutableComponent component = Texts.translatable("commands.croparia.crop.dump.singular", id);
            if (openFile) {
                MutableComponent openFileButton = Texts.openFileButton(dumped.toAbsolutePath().toString());
                component.append(" ").append(openFileButton);
            }
            messenger.success(component, true);
            return 1;
        } else {
            messenger.failure(Texts.translatable("commands.croparia.crop.dump.singular.fail", id));
            return 0;
        }
    }

    /* CREATE */
    public static <S> int createMelon(CommandContext<S> context, boolean client, boolean forced) throws CommandSyntaxException {
        try {
            DelegateSource<S> source = DelegateSource.of(context);
            ItemStack material = testMaterial(context);
            ResourceLocation materialId = Objects.requireNonNull(material.getItem().arch$registryName());
            int tier = testTier(context);
            Color color = testColor(context, "color");
            ResourceLocation id = testId(context, "id");
            if (id == null) id = CropariaIf.of(materialId.getPath());
            if (!forced && DgRegistries.MELONS.exists(id)) {
                MutableComponent crop = Texts.literal(id.toString());
                crop.withStyle(Texts.runCommand(CommonCommandRoot.commandRoot(client), "melon", id.toString()))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent rename = Texts.translatable("commands.croparia.crop.create.duplicated.rename")
                    .withStyle(Texts.suggestCommand(CommonCommandRoot.commandRoot(client), "melon", "create", color.toString(), id + "_"))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent replace = Texts.translatable("commands.croparia.crop.create.duplicated.replace")
                    .withStyle(Texts.suggestCommand(CommonCommandRoot.commandRoot(client), "melon", "create", color.toString(), id.toString(), "replace"))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent duplication = Texts.translatable("commands.croparia.crop.create.duplicated", crop, rename, replace);
                source.failure(duplication);
                return -1;
            }
            Melon melon = new Melon(id, new BlockMaterial(material), color, tier, null, new CropDependencies(Map.of(
                materialId.getNamespace(), material.getItem().getDescriptionId()
            )));
            Path result = DgRegistries.MELONS.dumpCrop(melon);
            MutableComponent resultComponent = Texts.literal(result.toString());
            if (client) {
                MutableComponent openFileButton = Texts.openFileButton(result.toAbsolutePath().toString());
                resultComponent.append(Texts.literal(" ")).append(openFileButton);
            }
            source.success(Texts.translatable("commands.croparia.crop.create.success"), true);
            return tier;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public static <S> int createCrop(CommandContext<S> context, boolean client, boolean forced) throws CommandSyntaxException {
        try {
            DelegateSource<S> source = DelegateSource.of(context);
            ItemStack material = testMaterial(context);
            ResourceLocation materialId = Objects.requireNonNull(material.getItem().arch$registryName());
            int tier = testTier(context);
            Color color = testColor(context, "color");
            String type = StringArgumentType.getString(context, "type");
            if (type == null) type = "crop";
            ResourceLocation id = testId(context, "id");
            if (id == null) id = CropariaIf.of(materialId.getPath());
            if (!forced && DgRegistries.CROPS.exists(id)) {
                MutableComponent crop = Texts.literal(id.toString());
                crop.withStyle(Texts.runCommand(CommonCommandRoot.commandRoot(client), "crop", id.toString()))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent rename = Texts.translatable("commands.croparia.crop.create.duplicated.rename")
                    .withStyle(Texts.suggestCommand(CommonCommandRoot.commandRoot(client), "crop", "create", color.toString(), type, id + "_"))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent replace = Texts.translatable("commands.croparia.crop.create.duplicated.replace")
                    .withStyle(Texts.suggestCommand(CommonCommandRoot.commandRoot(client), "crop", "create", color.toString(), type, id.toString(), "replace"))
                    .withStyle(Texts.inlineMouseBehavior());
                MutableComponent duplication = Texts.translatable("commands.croparia.crop.create.duplicated", crop, rename, replace);
                source.failure(duplication);
                return -1;
            }
            Crop crop = new Crop(id, new ItemMaterial(material), color, tier, type, null, new CropDependencies(
                materialId.getNamespace(), material.getItem().getDescriptionId()
            ));
            Path result = DgRegistries.CROPS.dumpCrop(crop);
            MutableComponent resultComponent = Texts.literal(result.toString());
            if (client) {
                MutableComponent openFileButton = Texts.openFileButton(result.toAbsolutePath().toString());
                resultComponent.append(" ").append(openFileButton);
            }
            source.success(Texts.translatable("commands.croparia.crop.create.success", resultComponent), true);
            return tier;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public static <S> ItemStack testMaterial(CommandContext<S> context) throws IllegalArgumentException {
        DelegateSource<S> source = DelegateSource.of(context);
        return source.getPlayer().map(LivingEntity::getMainHandItem).orElseThrow(() -> {
            source.failure(Texts.translatable("commands.croparia.crop.create.no_material"));
            return new IllegalArgumentException();
        });
    }

    public static <S> Color testColor(CommandContext<S> context, String key) throws IllegalArgumentException {
        String rawColor = StringArgumentType.getString(context, key);
        try {
            return new Color(rawColor);
        } catch (NumberFormatException e) {
            DelegateSource<S> source = DelegateSource.of(context);
            source.failure(Texts.translatable("commands.croparia.crop.create.invalid_color", rawColor));
            throw new IllegalArgumentException();
        }
    }

    public static <S> int testTier(CommandContext<S> context) throws CommandSyntaxException, IllegalArgumentException {
        DelegateSource<S> source = DelegateSource.of(context);
        Player player = source.getPlayerOrException();
        Item offhand = player.getOffhandItem().getItem();
        if (offhand instanceof Croparia croparia) {
            return croparia.getTier();
        } else {
            source.failure(Texts.translatable("commands.croparia.crop.create.invalid_croparia"));
            throw new IllegalArgumentException();
        }
    }

    public static <S> ResourceLocation testId(CommandContext<S> context, String key) throws IllegalArgumentException {
        ResourceLocation id = ResourceLocationArgument.getId(context, key);
        if (id != null && id.getNamespace().equals("minecraft")) {
            DelegateSource<S> source = DelegateSource.of(context);
            source.failure(Texts.translatable("commands.croparia.crop.create.namespace"));
            throw new IllegalArgumentException();
        }
        return id;
    }
}
