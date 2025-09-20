package cool.muyucloud.croparia.api.core.item;

import com.google.common.collect.ImmutableList;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.core.util.RecipeWizardGenerator;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.JarJarEntry;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class RecipeWizard extends Item {
    public static final ResourceLocation PACK_ID = CropariaIf.of("recipe_wizard");
    public static final OnLoadSupplier<Collection<RecipeWizardGenerator>> GENERATORS = OnLoadSupplier.of(() -> {
        for (JarJarEntry entry : PackHandler.getBuiltinGenerators().getOrDefault(PACK_ID, List.of())) {
            String name = entry.getEntry().getName();
            String prefix = "data-generators/%s/%s/".formatted(PACK_ID.getNamespace(), PACK_ID.getPath());
            Path target = CropariaIf.CONFIG.getFilePath().resolve("recipe_wizard/generators").resolve(name.substring(prefix.length()));
            try {
                entry.forInputStream(inputStream -> {
                    try (FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
                        inputStream.transferTo(outputStream);
                        outputStream.flush();
                    }
                });
            } catch (IOException e) {
                CropariaIf.LOGGER.error("Failed to move built-in recipe wizard template from %s to %s".formatted(name, target), e);
            }
        }
        Collection<RecipeWizardGenerator> generators = new ArrayList<>();
        try {
            FileUtil.forFilesIn(
                CropariaIf.CONFIG.getFilePath().resolve("recipe_wizard/generators").toFile(),
                file -> RecipeWizardGenerator.read(file).ifPresent(generator -> {
                    if (generator.isEnabled() && generator.isDependenciesAvailable()) {
                        generators.add(generator);
                    }
                })
            );
        } catch (IOException e) {
            CropariaIf.LOGGER.error("Failed to read recipe wizard generators", e);
        }
        return ImmutableList.copyOf(generators);
    });
    public static final Map<BlockInput, Function<UseOnContext, InteractionResult>> OPERATIONS = new HashMap<>();

    static {
        OPERATIONS.put(
            BlockInput.ofTag(TagKey.create(Registries.BLOCK, ResourceLocation.parse("croparia:ritual_stands"))),
            context -> {
                BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                return RitualStructure.TYPED_SERIALIZER.find(new RitualStructureContainer(state), context.getLevel()).map(structure -> {
                    structure.tryBuild(context.getLevel(), context.getClickedPos());
                    return (InteractionResult) InteractionResult.SUCCESS;
                }).orElse(InteractionResult.PASS);
            }
        );
    }

    public RecipeWizard(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.isCrouching()) {
            for (var entry : OPERATIONS.entrySet()) {
                if (entry.getKey().matches(level.getBlockState(context.getClickedPos()))) {
                    InteractionResult result = entry.getValue().apply(context);
                    if (!(result instanceof InteractionResult.Pass)) {
                        return result;
                    }
                }
            }
        }
        if (!level.isClientSide() || !player.isLocalPlayer()) {
            return InteractionResult.PASS;
        }
        BlockState target = context.getLevel().getBlockState(context.getClickedPos());
        for (RecipeWizardGenerator generator : GENERATORS.get()) {
            if (generator.matches(target)) {
                generator.handle(context);
                player.getCooldowns().addCooldown(context.getItemInHand(), 5);
                return InteractionResult.SUCCESS;
            }
        }
        Texts.overlay(player, Texts.translatable("overlay.croparia.recipe_wizard.error.no_match"));
        return InteractionResult.PASS;
    }
}
