package cool.muyucloud.croparia.api.core.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.AbstractFurnaceBlockEntityAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.core.block.Infusor;
import cool.muyucloud.croparia.api.core.block.RitualStand;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.RegexParser;
import cool.muyucloud.croparia.api.placeholder.RegexParserException;
import cool.muyucloud.croparia.api.placeholder.Template;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.Dependencies;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class RecipeWizardGenerator {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public static Optional<RecipeWizardGenerator> read(File file) {
        try {
            JsonElement json = DgReader.read(file);
            if (!json.isJsonObject() || json.getAsJsonObject().has("dependencies")) {
                if (!CodecUtil.decodeJson(json.getAsJsonObject().get("dependencies"), Dependencies.CODEC).mapOrElse(Dependencies::available, e -> {
                    CropariaIf.LOGGER.error("Failed to analyze dependencies of recipe wizard file %s".formatted(file));
                    CropariaIf.LOGGER.error(e.message());
                    return false;
                })) {
                    CropariaIf.LOGGER.warn("Skipped loading recipe wizard file %s due to missing or bad dependencies".formatted(file));
                    return Optional.empty();
                }
            }
            return CodecUtil.decodeJson(json, CODEC).mapOrElse(Optional::of, error -> {
                CropariaIf.LOGGER.error("Failed to compile recipe wizard file %s".formatted(file), error);
                return Optional.empty();
            });
        } catch (IOException | JsonParseException e) {
            CropariaIf.LOGGER.error("Failed to read recipe wizard file %s".formatted(file), e);
            return Optional.empty();
        }
    }

    protected static final Map<ResourceLocation, ArrayList<Placeholder<UseOnContext>>> EXTENSIONS = new HashMap<>();

    public static Placeholder<UseOnContext> register(ResourceLocation id, Placeholder<UseOnContext> placeholder) {
        ArrayList<Placeholder<UseOnContext>> list = EXTENSIONS.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(placeholder);
        list.trimToSize();
        return placeholder;
    }

    public static Placeholder<UseOnContext> register(ResourceLocation id, Pattern key, Function<UseOnContext, String> function) {
        return register(id, key, function, Placeholder.STRING);
    }

    public static <T> Placeholder<UseOnContext> register(ResourceLocation id, Pattern key, Function<UseOnContext, T> function, RegexParser<T> parser) {
        return register(id, Placeholder.build(builder -> builder.then(key, function, parser)));
    }

    public static final Placeholder<UseOnContext> TIMESTAMP = register(
        ResourceLocation.parse("default"),
        Pattern.compile("^datetime$"),
        context -> LocalDateTime.now().format(FORMATTER)
    );
    public static final Placeholder<UseOnContext> MAIN_HAND = register(
        ResourceLocation.parse("default"), Pattern.compile("^main_hand$"), context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new ReplaceException();
            } else {
                return ItemOutput.of(stack);
            }
        }, Placeholder.ITEM_OUTPUT
    );
    public static final Placeholder<UseOnContext> OFF_HAND = register(
        ResourceLocation.parse("default"), Pattern.compile("^off_hand$"), context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new ReplaceException();
            } else {
                return ItemOutput.of(stack);
            }
        }, Placeholder.ITEM_OUTPUT
    );
    public static final Placeholder<UseOnContext> ITEM = register(
        ResourceLocation.parse("default"), Pattern.compile("^item$"), context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item"));
                throw new ReplaceException();
            } else {
                return ItemOutput.of(entities.getFirst().getItem());
            }
        }, Placeholder.ITEM_OUTPUT
    );
    public static final Placeholder<UseOnContext> BLOCK = register(
        ResourceLocation.parse("default"), Pattern.compile("^block$"), context -> {
            Level level = context.getLevel();
            BlockState block = level.getBlockState(context.getClickedPos());
            if (block.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block"));
                throw new ReplaceException();
            }
            return BlockOutput.of(block);
        }, Placeholder.BLOCK_OUTPUT
    );
    public static final Placeholder<UseOnContext> NEIGHBOR = register(ResourceLocation.parse("default"), Pattern.compile("^neighbor$"), context -> {
        Level level = context.getLevel();
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
            if (!state.isAir()) {
                return BlockOutput.of(state);
            }
        }
        assert context.getPlayer() != null;
        Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
        throw new ReplaceException();
    }, Placeholder.BLOCK_OUTPUT);
    public static final Placeholder<UseOnContext> BELOW = register(ResourceLocation.parse("default"), Pattern.compile("^below$"), context -> {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos().below());
        if (state.isAir()) {
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block"));
            throw new ReplaceException();
        }
        return BlockOutput.of(state);
    }, Placeholder.BLOCK_OUTPUT);
    public static final Placeholder<UseOnContext> INFUSOR_ELEMENT = register(CropariaIf.of("infusor"), Pattern.compile("^infusor_element$"), context -> {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof Infusor) {
            Element element = state.getValue(Infusor.ELEMENT);
            if (element != Element.EMPTY) {
                return element.getSerializedName();
            }
        }
        assert context.getPlayer() != null;
        Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.infusor.missing.element"));
        throw new ReplaceException();
    });
    public static final Placeholder<UseOnContext> RITUAL_INPUT = register(CropariaIf.of("ritual"), Pattern.compile("^ritual_input$"), context -> {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RitualStand) {
            RitualStructureContainer container = new RitualStructureContainer(state);
            Optional<RitualStructure> structure = Recipes.RITUAL_STRUCTURE.find(container, level);
            Optional<BlockState> input = structure.flatMap(s -> s.validate(pos, level).getStates().stream().filter(candidate -> !candidate.isAir()).findFirst());
            if (input.isPresent()) {
                return BlockOutput.of(input.get());
            }
        }
        assert context.getPlayer() != null;
        Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.ritual.missing.block"));
        throw new ReplaceException();
    }, Placeholder.BLOCK_OUTPUT);
    public static final Placeholder<UseOnContext> SOAK_ELEMENT = register(CropariaIf.of("soak"), Pattern.compile("^soak_element$"), context -> {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos().above());
        if (state.getBlock() instanceof Infusor infusor) {
            Element element = state.getValue(Infusor.ELEMENT);
            return element.getSerializedName();
        } else {
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.soak.missing.element"));
            throw new ReplaceException();
        }
    });
    public static final Placeholder<UseOnContext> FURNACE_INPUT = register(ResourceLocation.parse("furnace"), Pattern.compile("^furnace_input$"), context -> {
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack stack = furnace.getItem(0);
            if (!stack.isEmpty()) {
                return ItemOutput.of(stack);
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
            }
        } else {
            Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
        }
        throw new ReplaceException();
    }, Placeholder.ITEM_OUTPUT);
    public static final Placeholder<UseOnContext> FURNACE_TIME = register(ResourceLocation.parse("furnace"), Pattern.compile("^furnace_time$"), context -> {
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(be instanceof AbstractFurnaceBlockEntity furnace)) {
            Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            throw new ReplaceException();
        }
        ItemStack input = furnace.getItem(0);
        if (input.isEmpty()) {
            Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
            throw new ReplaceException();
        }
        ItemStack fuel = furnace.getItem(1);
        if (fuel.isEmpty()) {
            Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_fuel"));
            throw new ReplaceException();
        }
        return String.valueOf(((AbstractFurnaceBlockEntityAccess) furnace).cif$getBurnDuration(context.getLevel(), fuel) * fuel.getCount());
    });

    protected static Collection<Placeholder<UseOnContext>> getExtensions(ResourceLocation id) {
        return EXTENSIONS.getOrDefault(id, new ArrayList<>());
    }

    public static final MapCodec<RecipeWizardGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(RecipeWizardGenerator::isEnabled),
        Dependencies.CODEC.optionalFieldOf("dependencies", Dependencies.EMPTY).forGetter(RecipeWizardGenerator::getDependencies),
        BlockInput.CODEC.fieldOf("block").forGetter(RecipeWizardGenerator::getBlock),
        Template.CODEC.fieldOf("path").forGetter(RecipeWizardGenerator::getPath),
        CodecUtil.listOf(ResourceLocation.CODEC).optionalFieldOf("extensions", List.of()).forGetter(RecipeWizardGenerator::getExtensions),
        Template.CODEC.fieldOf("template").forGetter(RecipeWizardGenerator::getTemplate)
    ).apply(instance, RecipeWizardGenerator::new));

    private final boolean enabled;
    private final Dependencies dependencies;
    private final BlockInput block;
    private final Template path;
    private final List<ResourceLocation> extensions;
    private final Template template;
    private final transient Placeholder<UseOnContext> placeholder;

    public RecipeWizardGenerator(boolean enabled, Dependencies dependencies, BlockInput block, Template path, List<ResourceLocation> extensions, Template template) {
        this.enabled = enabled;
        this.dependencies = dependencies;
        this.block = block;
        this.path = path;
        this.extensions = extensions instanceof ImmutableList<ResourceLocation> immutable ? immutable : ImmutableList.copyOf(extensions);
        this.template = template;
        this.placeholder = Placeholder.build(builder -> {
            Collection<Placeholder<UseOnContext>> defaults = getExtensions(ResourceLocation.parse("default"));
            for (Placeholder<UseOnContext> p : defaults) {
                builder.overwrite(p);
            }
            for (ResourceLocation id : this.getExtensions()) {
                Collection<Placeholder<UseOnContext>> extension = getExtensions(id);
                for (Placeholder<UseOnContext> p : extension) {
                    builder.overwrite(p);
                }
            }
            return builder;
        });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BlockInput getBlock() {
        return block;
    }

    public Template getPath() {
        return path;
    }

    public Dependencies getDependencies() {
        return dependencies;
    }

    public List<ResourceLocation> getExtensions() {
        return extensions;
    }

    public Template getTemplate() {
        return template;
    }

    public Placeholder<UseOnContext> getPlaceholder() {
        return placeholder;
    }

    public boolean matches(BlockState state) {
        return this.getBlock().matches(state);
    }

    public void handle(UseOnContext context) {
        Player player = Objects.requireNonNull(context.getPlayer());
        if (this.isEnabled()) {
            try {
                String path = this.getPath().replace(context, this.getPlaceholder());
                String template = this.getTemplate().replace(context, this.getPlaceholder());
                Path result = CropariaIf.CONFIG.getRecipeWizard().resolve(path);
                FileUtil.write(result.toFile(), template, true);
                String s = result.toString();
                Component c = Texts.literal(s).withStyle(Texts.openFile(s)).withStyle(Texts.inlineMouseBehavior());
                Texts.chat(player, Texts.translatable("chat.croparia.recipe_wizard.success", c));
            } catch (ReplaceException ignored) {
                // Termination caused by missing data, message already sent in placeholder
            } catch (RegexParserException | IOException e) {
                Texts.chat(player, Texts.translatable("overlay.croparia.recipe_wizard.failed").withStyle(ChatFormatting.RED));
                CropariaIf.LOGGER.error("Failed to generate recipe", e);
            }
        }
    }
}
