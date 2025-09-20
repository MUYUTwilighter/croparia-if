package cool.muyucloud.croparia.api.core.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.AbstractFurnaceBlockEntityAccess;
import cool.muyucloud.croparia.access.StateHolderAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.core.block.Infusor;
import cool.muyucloud.croparia.api.core.block.RitualStand;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.generator.util.DgReader;
import cool.muyucloud.croparia.api.generator.util.Placeholder;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.entry.BlockOutput;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.Dependencies;
import cool.muyucloud.croparia.util.FileUtil;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;

@SuppressWarnings("unused")
public class RecipeWizardGenerator {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public static Optional<RecipeWizardGenerator> read(File file) {
        try {
            JsonElement json = DgReader.read(file);
            return Optional.of(CodecUtil.decodeJson(json, CODEC));
        } catch (Throwable t) {
            CropariaIf.LOGGER.error("Failed to compile recipe wizard file %s".formatted(file), t);
            return Optional.empty();
        }
    }

    protected static final Map<ResourceLocation, ArrayList<Placeholder<UseOnContext>>> EXTENSIONS = new HashMap<>();

    public static Placeholder<UseOnContext> register(ResourceLocation id, String regex, Function<UseOnContext, String> mapper) {
        return register(id, regex, (matcher, context) -> mapper.apply(context));
    }

    public static Placeholder<UseOnContext> register(ResourceLocation id, String regex, BiFunction<Matcher, UseOnContext, String> mapper) {
        return register(id, Placeholder.of(regex, mapper));
    }

    public static Placeholder<UseOnContext> register(ResourceLocation id, Placeholder<UseOnContext> placeholder) {
        ArrayList<Placeholder<UseOnContext>> list = EXTENSIONS.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(placeholder);
        list.trimToSize();
        return placeholder;
    }

    public static final Placeholder<UseOnContext> TIMESTAMP = register(
        ResourceLocation.parse("default"), "\\{datetime}", context -> LocalDateTime.now().format(FORMATTER)
    );
    public static final Placeholder<UseOnContext> MAIN_HAND = register(
        ResourceLocation.tryParse("default"), "\\{main_hand}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else {
                return CodecUtil.encodeJson(new ItemOutput(stack), ItemOutput.CODEC).toString();
            }
        }
    );
    public static final Placeholder<UseOnContext> MAIN_HAND_ID = register(
        ResourceLocation.tryParse("default"), "\\{main_hand_id}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).toString();
        }
    );
    public static final Placeholder<UseOnContext> MAIN_HAND_COUNT = register(
        ResourceLocation.tryParse("default"), "\\{main_hand_count}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else return String.valueOf(stack.getCount());
        }
    );
    public static final Placeholder<UseOnContext> MAIN_HAND_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{main_hand_namespace}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).getNamespace();
        }
    );
    public static final Placeholder<UseOnContext> MAIN_HAND_PATH = register(
        ResourceLocation.tryParse("default"), "\\{main_hand_path}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).getPath();
        }
    );
    public static final Placeholder<UseOnContext> MAIN_HAND_COMPONENTS = register(
        ResourceLocation.tryParse("default"), "\\{main_hand_components}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.main_hand"));
                throw new IllegalStateException();
            } else return CodecUtil.encodeJson(stack.getComponentsPatch(), DataComponentPatch.CODEC).toString();
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND = register(
        ResourceLocation.tryParse("default"), "\\{off_hand}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else {
                return CodecUtil.encodeJson(new ItemOutput(stack), ItemOutput.CODEC).toString();
            }
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND_ID = register(
        ResourceLocation.tryParse("default"), "\\{off_hand_id}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).toString();
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND_COUNT = register(
        ResourceLocation.tryParse("default"), "\\{off_hand_count}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else return String.valueOf(stack.getCount());
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{off_hand_namespace}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).getNamespace();
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND_PATH = register(
        ResourceLocation.tryParse("default"), "\\{off_hand_namespace}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else return Objects.requireNonNull(stack.getItem().arch$registryName()).getPath();
        }
    );
    public static final Placeholder<UseOnContext> OFF_HAND_COMPONENTS = register(
        ResourceLocation.tryParse("default"), "\\{off_hand_components}", context -> {
            ItemStack stack = Objects.requireNonNull(context.getPlayer()).getItemInHand(InteractionHand.OFF_HAND);
            if (stack.isEmpty()) {
                Texts.overlay(context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.default.missing.off_hand"));
                throw new IllegalStateException();
            } else return CodecUtil.encodeJson(stack.getComponentsPatch(), DataComponentPatch.CODEC).toString();
        }
    );
    public static final Placeholder<UseOnContext> ITEM = register(
        ResourceLocation.tryParse("default"), "\\{item}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else {
                return CodecUtil.encodeJson(new ItemOutput(entities.getFirst().getItem()), ItemOutput.CODEC).toString();
            }
        }
    );
    public static final Placeholder<UseOnContext> ITEM_ID = register(
        ResourceLocation.tryParse("default"), "\\{item}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else
                return Objects.requireNonNull(entities.getFirst().getItem().getItem().arch$registryName()).toString();
        }
    );
    public static final Placeholder<UseOnContext> ITEM_COUNT = register(
        ResourceLocation.tryParse("default"), "\\{item_count}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else
                return String.valueOf(entities.getFirst().getItem().getCount());
        }
    );
    public static final Placeholder<UseOnContext> ITEM_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{item_namespace}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else return Objects.requireNonNull(
                entities.getFirst().getItem().getItem().arch$registryName()
            ).getNamespace();
        }
    );
    public static final Placeholder<UseOnContext> ITEM_PATH = register(
        ResourceLocation.tryParse("default"), "\\{item_path}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else return Objects.requireNonNull(entities.getFirst().getItem().getItem().arch$registryName()).getPath();
        }
    );
    public static final Placeholder<UseOnContext> ITEM_COMPONENTS = register(
        ResourceLocation.tryParse("default"), "\\{item_components}", context -> {
            List<ItemEntity> entities = context.getLevel().getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                AABB.encapsulatingFullBlocks(context.getClickedPos(), context.getClickedPos().above()),
                item -> !item.getItem().isEmpty()
            );
            if (entities.isEmpty()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.target_item")
                );
                throw new IllegalStateException();
            } else return CodecUtil.encodeJson(entities.getFirst().getItem().getComponentsPatch(),
                DataComponentPatch.CODEC).toString();
        }
    );
    public static final Placeholder<UseOnContext> BLOCK = register(
        ResourceLocation.tryParse("default"), "\\{block}", context -> {
            Level level = context.getLevel();
            BlockState block = level.getBlockState(context.getClickedPos());
            if (block.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            return CodecUtil.encodeJson(BlockOutput.of(block), BlockOutput.CODEC).toString();
        }
    );
    public static final Placeholder<UseOnContext> BLOCK_ID = register(
        ResourceLocation.tryParse("default"), "\\{block_id}", context -> {
            Level level = context.getLevel();
            Block block = level.getBlockState(context.getClickedPos()).getBlock();
            if (block == Blocks.AIR) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).toString();
        }
    );
    public static final Placeholder<UseOnContext> BLOCK_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{block_namespace}", context -> {
            Level level = context.getLevel();
            Block block = level.getBlockState(context.getClickedPos()).getBlock();
            if (block == Blocks.AIR) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).getNamespace();
        }
    );
    public static final Placeholder<UseOnContext> BLOCK_PATH = register(
        ResourceLocation.tryParse("default"), "\\{block_path}", context -> {
            Level level = context.getLevel();
            Block block = level.getBlockState(context.getClickedPos()).getBlock();
            if (block == Blocks.AIR) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).getPath();
        }
    );
    public static final Placeholder<UseOnContext> BLOCK_PROPERTIES = register(
        ResourceLocation.tryParse("default"), "\\{block_properties}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof AirBlock) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            @SuppressWarnings("unchecked")
            StateHolderAccess<BlockState> access = (StateHolderAccess<BlockState>) level.getBlockState(context.getClickedPos());
            JsonObject json = new JsonObject();
            access.cif$getProperties().forEach(json::addProperty);
            return json.toString();
        }
    );
    public static final Placeholder<UseOnContext> NEIGHBOR = register(
        ResourceLocation.tryParse("default"), "\\{neighbor}", context -> {
            Level level = context.getLevel();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
                if (!state.isAir()) {
                    return CodecUtil.encodeJson(BlockOutput.of(state), BlockOutput.CODEC).toString();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> NEIGHBOR_ID = register(
        ResourceLocation.tryParse("default"), "\\{neighbor_id}", context -> {
            Level level = context.getLevel();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
                if (!state.isAir()) {
                    return Objects.requireNonNull(state.getBlock().arch$registryName()).toString();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> NEIGHBOR_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{neighbor_namespace}", context -> {
            Level level = context.getLevel();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
                if (!state.isAir()) {
                    return Objects.requireNonNull(state.getBlock().arch$registryName()).getNamespace();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> NEIGHBOR_PATH = register(
        ResourceLocation.tryParse("default"), "\\{neighbor_path}", context -> {
            Level level = context.getLevel();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
                if (!state.isAir()) {
                    return Objects.requireNonNull(state.getBlock().arch$registryName()).getPath();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
            throw new IllegalStateException();
        }
    );
    @SuppressWarnings("unchecked")
    public static final Placeholder<UseOnContext> NEIGHBOR_PROPERTIES = register(
        ResourceLocation.tryParse("default"), "\\{neighbor_properties}", context -> {
            Level level = context.getLevel();
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockState state = level.getBlockState(context.getClickedPos().offset(direction.getUnitVec3i()));
                if (!state.isAir()) {
                    JsonObject properties = new JsonObject();
                    ((StateHolderAccess<BlockState>) state).cif$getProperties().forEach(properties::addProperty);
                    return properties.toString();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor"));
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> BELOW = register(
        ResourceLocation.tryParse("default"), "\\{below}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos().below());
            if (state.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.block")
                );
                throw new IllegalStateException();
            }
            return CodecUtil.encodeJson(BlockOutput.of(state), BlockOutput.CODEC).toString();
        }
    );
    public static final Placeholder<UseOnContext> BELOW_ID = register(
        ResourceLocation.tryParse("default"), "\\{below_id}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos().below());
            Block block = state.getBlock();
            if (state.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).toString();
        }
    );
    public static final Placeholder<UseOnContext> BELOW_NAMESPACE = register(
        ResourceLocation.tryParse("default"), "\\{below_namespace}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos().below());
            Block block = state.getBlock();
            if (state.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.neighbor")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).getNamespace();
        }
    );
    public static final Placeholder<UseOnContext> BELOW_PATH = register(
        ResourceLocation.tryParse("default"), "\\{below_path}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos().below());
            Block block = state.getBlock();
            if (state.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.opposite")
                );
                throw new IllegalStateException();
            }
            return Objects.requireNonNull(block.arch$registryName()).getPath();
        }
    );
    public static final Placeholder<UseOnContext> BELOW_PROPERTIES = register(
        ResourceLocation.tryParse("default"), "\\{below_properties}", context -> {
            Level level = context.getLevel();
            BlockState state = level.getBlockState(context.getClickedPos().below());
            Block block = state.getBlock();
            if (state.isAir()) {
                assert context.getPlayer() != null;
                Texts.overlay(context.getPlayer(),
                    Texts.translatable("overlay.croparia.recipe_wizard.default.missing.opposite")
                );
                throw new IllegalStateException();
            }
            @SuppressWarnings("unchecked")
            StateHolderAccess<BlockState> access = (StateHolderAccess<BlockState>) level.getBlockState(context.getClickedPos());
            JsonObject json = new JsonObject();
            access.cif$getProperties().forEach(json::addProperty);
            return json.toString();
        }
    );
    public static final Placeholder<UseOnContext> INFUSOR_ELEMENT = register(
        CropariaIf.of("infusor"), "\\{infusor_element}", context -> {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof Infusor) {
                Element element = state.getValue(Infusor.ELEMENT);
                if (element != Element.EMPTY) {
                    return element.getKey().toString();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.infusor.missing.element")
            );
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> RITUAL_INPUT = register(
        CropariaIf.of("ritual"), "\\{ritual_input}", context -> {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof RitualStand) {
                RitualStructureContainer container = new RitualStructureContainer(state);
                Optional<RitualStructure> structure = Recipes.RITUAL_STRUCTURE.find(container, level);
                Optional<BlockState> input = structure.flatMap(s ->
                    s.validate(pos, level).getStates().stream().filter(candidate -> !candidate.isAir()).findFirst());
                if (input.isPresent()) {
                    return CodecUtil.encodeJson(BlockOutput.of(input.get()), BlockOutput.CODEC).toString();
                }
            }
            assert context.getPlayer() != null;
            Texts.overlay(context.getPlayer(),
                Texts.translatable("overlay.croparia.recipe_wizard.ritual.missing.block"));
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> SOAK_ELEMENT = register(
        CropariaIf.of("soak"), "\\{soak_element}", context -> {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos().above());
            if (state.getBlock() instanceof Infusor infusor) {
                Element element = state.getValue(Infusor.ELEMENT);
                return element.getKey().toString();
            } else {
                assert context.getPlayer() != null;
                Texts.overlay(
                    context.getPlayer(), Texts.translatable("overlay.croparia.recipe_wizard.soak.missing.element")
                );
                throw new IllegalStateException();
            }
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return CodecUtil.encodeJson(new ItemOutput(stack), ItemOutput.CODEC).toString();
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT_ID = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input_id}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return Objects.requireNonNull(stack.getItem().arch$registryName()).toString();
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT_COUNT = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input_count}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return String.valueOf(stack.getCount());
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT_NAMESPACE = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input_namespace}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return Objects.requireNonNull(stack.getItem().arch$registryName()).getNamespace();
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT_PATH = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input_path}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return Objects.requireNonNull(stack.getItem().arch$registryName()).getPath();
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_INPUT_COMPONENTS = register(
        ResourceLocation.parse("furnace"), "\\{furnace_input_components}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack stack = furnace.getItem(0);
                if (!stack.isEmpty()) {
                    return CodecUtil.encodeJson(stack.getComponentsPatch(), DataComponentPatch.CODEC).toString();
                } else {
                    Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                }
            } else {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
            }
            throw new IllegalStateException();
        }
    );
    public static final Placeholder<UseOnContext> FURNACE_TIME = register(
        ResourceLocation.parse("furnace"), "\\{furnace_time}", context -> {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (!(be instanceof AbstractFurnaceBlockEntity furnace)) {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_furnace"));
                throw new IllegalStateException();
            }
            ItemStack input = furnace.getItem(0);
            if (input.isEmpty()) {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_input"));
                throw new IllegalStateException();
            }
            ItemStack fuel = furnace.getItem(1);
            if (fuel.isEmpty()) {
                Texts.overlay(Objects.requireNonNull(context.getPlayer()), Texts.translatable("overlay.croparia.recipe_wizard.furnace.no_fuel"));
                throw new IllegalStateException();
            }
            return String.valueOf(((AbstractFurnaceBlockEntityAccess) furnace).cif$getBurnDuration(context.getLevel(), fuel) * fuel.getCount());
        }
    );

    protected static Collection<Placeholder<UseOnContext>> getExtensions(ResourceLocation id) {
        return EXTENSIONS.getOrDefault(id, new ArrayList<>());
    }

    public static final MapCodec<RecipeWizardGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(RecipeWizardGenerator::isEnabled),
        Dependencies.CODEC.optionalFieldOf("dependencies", Dependencies.EMPTY).forGetter(RecipeWizardGenerator::getDependencies),
        BlockInput.CODEC.fieldOf("block").forGetter(RecipeWizardGenerator::getBlock),
        Codec.STRING.fieldOf("path").forGetter(RecipeWizardGenerator::getPath),
        CodecUtil.listOf(ResourceLocation.CODEC).optionalFieldOf("extensions", List.of()).forGetter(RecipeWizardGenerator::getExtensions),
        Codec.STRING.fieldOf("template").forGetter(RecipeWizardGenerator::getTemplate)
    ).apply(instance, RecipeWizardGenerator::new));

    private final boolean enabled;
    private final Dependencies dependencies;
    private final BlockInput block;
    private final String path;
    private final List<ResourceLocation> extensions;
    private final String template;
    private final transient LazySupplier<Boolean> dependenciesAvailable = LazySupplier.of(() -> this.getDependencies().available());

    public RecipeWizardGenerator(boolean enabled, Dependencies dependencies, BlockInput block, String path, List<ResourceLocation> extensions, String template) {
        this.enabled = enabled;
        this.dependencies = dependencies;
        this.block = block;
        this.path = path;
        this.extensions = extensions instanceof ImmutableList<ResourceLocation> immutable ? immutable : ImmutableList.copyOf(extensions);
        this.template = template;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BlockInput getBlock() {
        return block;
    }

    public String getPath() {
        return path;
    }

    public Dependencies getDependencies() {
        return dependencies;
    }

    public boolean isDependenciesAvailable() {
        return this.dependenciesAvailable.get();
    }

    public List<ResourceLocation> getExtensions() {
        return extensions;
    }

    public String getTemplate() {
        return template;
    }

    public boolean matches(BlockState state) {
        return this.getBlock().matches(state);
    }

    public void handle(UseOnContext context) {
        Player player = Objects.requireNonNull(context.getPlayer());
        if (this.isEnabled() && this.isDependenciesAvailable()) {
            String template = this.getTemplate();
            String path = this.getPath();
            try {
                for (ResourceLocation id : this.getExtensions()) {
                    Collection<Placeholder<UseOnContext>> extension = getExtensions(id);
                    for (Placeholder<UseOnContext> placeholder : extension) {
                        template = placeholder.mapAll(template, context);
                        path = placeholder.mapAll(path, context);
                    }
                }
                for (Placeholder<UseOnContext> placeholder : getExtensions(ResourceLocation.tryParse("default"))) {
                    template = placeholder.mapAll(template, context);
                    path = placeholder.mapAll(path, context);
                }
                Path result = CropariaIf.CONFIG.getRecipeWizard().resolve(path);
                FileUtil.write(result.toFile(), template, true);
                String s = result.toString();
                Component c = Texts.literal(s).withStyle(Texts.openFile(s)).withStyle(Texts.inlineMouseBehavior());
                Texts.chat(player, Texts.translatable("chat.croparia.recipe_wizard.success", c));
            } catch (IllegalStateException ignored) {
                // Termination caused by missing data, message already sent in placeholder
            } catch (Throwable t) {
                Texts.chat(player, Texts.translatable("overlay.croparia.recipe_wizard.failed").withStyle(ChatFormatting.RED));
                CropariaIf.LOGGER.error("Failed to generate recipe", t);
            }
        }
    }
}
