package cool.muyucloud.croparia.api.core.item;

import com.google.common.collect.ImmutableList;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.block.Infusor;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.core.util.RecipeWizardGenerator;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.generator.pack.PackHandler;
import cool.muyucloud.croparia.api.generator.util.JarJarEntry;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class RecipeWizard extends Item {
    public static final Identifier PACK_ID = CropariaIf.of("recipe_wizard");
    public static final OnLoadSupplier<Collection<RecipeWizardGenerator>> GENERATORS = OnLoadSupplier.of(() -> {
        for (JarJarEntry entry : PackHandler.getBuiltinGenerators(PACK_ID)) {
            String name = entry.getJarEntry().getName();
            String prefix = "data-generators/%s/%s/".formatted(PACK_ID.getNamespace(), PACK_ID.getPath());
            Path target = CropariaIf.CONFIG.getFilePath().resolve("recipe_wizard/generators").resolve(name.substring(prefix.length()));
            try {
                entry.forInputStream(inputStream -> FileUtil.transfer(inputStream, target.toFile(), false));
            } catch (IOException e) {
                CropariaIf.LOGGER.error("Failed to move built-in recipe wizard template from %s to %s".formatted(name, target), e);
            }
        }
        Collection<RecipeWizardGenerator> generators = new ArrayList<>();
        try {
            FileUtil.forFilesIn(
                CropariaIf.CONFIG.getFilePath().resolve("recipe_wizard/generators").toFile(),
                file -> RecipeWizardGenerator.read(file).ifPresent(generator -> {
                    if (generator.isEnabled()) {
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
            // Build Ritual Structure
            BlockInput.ofTag(TagKey.create(Registries.BLOCK, Identifier.parse("croparia:ritual_stands"))),
            context -> {
                BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                Optional<RitualStructure> structure = RitualStructure.TYPED_SERIALIZER.find(new RitualStructureContainer(state), context.getLevel());
                if (structure.isEmpty()) {
                    return InteractionResult.PASS;
                }
                structure.get().tryBuild(context.getLevel(), context.getClickedPos());
                return InteractionResult.SUCCESS;
            }
        );
        OPERATIONS.put(
            // Shuffle Infusor element
            BlockInput.of(CropariaBlocks.INFUSOR.getId()),
            context -> {
                if (Infusor.ELEMENT.getPossibleValues().isEmpty()) return InteractionResult.PASS;
                BlockState state = context.getLevel().getBlockState(context.getClickedPos());
                Iterator<Element> iterator = Infusor.ELEMENT.getPossibleValues().iterator();
                while (iterator.hasNext()) {
                    Element tmp = iterator.next();
                    if (tmp == state.getValue(Infusor.ELEMENT)) {
                        Element next = iterator.hasNext() ? iterator.next() : Infusor.ELEMENT.getPossibleValues().iterator().next();
                        context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.setValue(Infusor.ELEMENT, next));
                        return InteractionResult.SUCCESS;
                    }
                }
                // No matching element
                return InteractionResult.PASS;
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
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
            }
        }
        // Client-only, we don't want generated files spam on server, while client never get the files.
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
