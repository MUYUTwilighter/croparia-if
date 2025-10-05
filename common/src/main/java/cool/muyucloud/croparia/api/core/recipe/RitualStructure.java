package cool.muyucloud.croparia.api.core.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.structure.Char3D;
import cool.muyucloud.croparia.api.recipe.structure.MarkedChar3D;
import cool.muyucloud.croparia.api.recipe.structure.MarkedTransformableChar3D;
import cool.muyucloud.croparia.registry.CropariaBlocks;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class RitualStructure implements DisplayableRecipe<RitualStructureContainer> {
    public static final TypedSerializer<RitualStructure> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("ritual_structure"), RitualStructure.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockInput.CODEC.fieldOf("ritual").forGetter(RitualStructure::getRitual),
            Codec.unboundedMap(CodecUtil.CHAR, BlockInput.CODEC).fieldOf("keys").forGetter(RitualStructure::getKeys),
            Char3D.CODEC.fieldOf("pattern").forGetter(RitualStructure::getPattern)
        ).apply(instance, RitualStructure::new)), TypedSerializer.ALWAYS
    );
    private static final OnLoadSupplier<Map<RitualStructure, Mappable<ItemStack>>> STRUCTURES = OnLoadSupplier.of(() -> {
        ImmutableMap.Builder<RitualStructure, Mappable<ItemStack>> builder = ImmutableMap.builder();
        TYPED_SERIALIZER.getStations().forEach(mappable -> {
            ResourceLocation id = mappable.get().getItem().arch$registryName();
            CropariaIf.ifServer(server -> server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).map(RecipeHolder::value).ifPresent(recipe -> {
                if (recipe instanceof RitualStructure structure) builder.put(structure, mappable);
            }));
        });
        return builder.build();
    });
    public static final LazySupplier<ItemStack> STACK_INPUT = LazySupplier.of(() -> {
        ItemStack stack = CropariaItems.PLACEHOLDER.get().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME, Constants.INPUT_BLOCK);
        return stack;
    });

    private final BlockInput ritual;
    @NotNull
    private final ImmutableMap<Character, BlockInput> keys;
    @NotNull
    private final MarkedTransformableChar3D patterns;
    private final transient LazySupplier<List<List<ItemStack>>> inputsCache = LazySupplier.of(() -> {
        ImmutableList.Builder<List<ItemStack>> builder = ImmutableList.builder();
        this.getPattern().forEachChar((key, count) -> {
            if (key == '*') {
                builder.add(this.getRitual().getDisplayStacks());
            } else if (key != '.' && key != ' ' && key != '$') {
                BlockInput input = this.getKeys().get(key);
                if (input != null)
                    builder.add(input.getDisplayStacks().stream().map(stack -> stack.copyWithCount(count)).toList());
                else builder.add(List.of());
            }
        });
        return builder.build();
    });

    public RitualStructure(BlockInput ritual, @NotNull Map<Character, BlockInput> keyDeclarations, Char3D rawPattern) {
        this.ritual = ritual;
        // Validate key declarations
        Map<Character, BlockInput> keys = new HashMap<>();
        for (Character c : keyDeclarations.keySet()) {
            if (c == '*' || c == '$' || c == '.' || c == ' ') throw new IllegalArgumentException("Preserved key: " + c);
            keys.put(c, keyDeclarations.get(c));
        }
        this.keys = ImmutableMap.copyOf(keys);
        // Validate pattern keys
        for (char c : rawPattern.chars()) {
            if (!this.keys.containsKey(c) && c != '$' && c != '*' && c != '.' && c != ' ')
                throw new IllegalArgumentException("Unknown key: " + c);
        }
        if (!rawPattern.contains('$'))
            throw new IllegalArgumentException("Ritual structure must contains a block input ($).");
        // Validate pattern structure
        Vec3i mark = rawPattern.find('*').orElseThrow(() -> new IllegalArgumentException("Ritual structure must contains a ritual mark (*)."));
        this.patterns = new MarkedTransformableChar3D(rawPattern, mark);
    }

    public BlockInput getRitual() {
        return ritual;
    }

    @Override
    public @NotNull List<List<ItemStack>> getInputs() {
        return this.inputsCache.get();
    }

    @Override
    public @NotNull List<List<ItemStack>> getOutputs() {
        return List.of(this.getRitual().getDisplayStacks());
    }

    public @NotNull Result matchTransformed(BlockPos origin, Level level, MarkedChar3D pattern, BlockState ritualBlock) {
        origin = pattern.getOriginInWorld(origin);
        List<BlockPos> inputPositions = new ArrayList<>();
        List<BlockState> inputBlocks = new ArrayList<>();
        for (int x = 0; x < pattern.xSize(); x++) {
            for (int y = 0; y < pattern.ySize(); y++) {
                for (int z = 0; z < pattern.zSize(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    char key = pattern.get(x, y, z);
                    if (key == '$') {
                        inputPositions.add(pos);
                        inputBlocks.add(state);
                    } else if (key == '*') {
                        if (!state.equals(ritualBlock)) {
                            return Result.FAIL;
                        }
                    } else if (key == '.') {
                        if (!state.isAir()) {
                            return Result.FAIL;
                        }
                    } else if (key == ' ') {
                        BlockInput.ANY.matches(state);
                    } else {
                        BlockInput input = this.keys.get(key);
                        if (input == null || !input.matches(state)) {
                            return Result.FAIL;
                        }
                    }
                }
            }
        }
        return result(inputBlocks, () -> {
            for (BlockPos pos : inputPositions) {
                level.destroyBlock(pos, false);
            }
        });
    }

    public Result validate(BlockPos origin, Level level) {
        for (MarkedChar3D pattern : this.patterns) {
            Result result = this.matchTransformed(origin, level, pattern, level.getBlockState(origin));
            if (result != Result.FAIL) return result;
        }
        return Result.FAIL;
    }

    public @NotNull Map<Character, BlockInput> getKeys() {
        return this.keys;
    }

    public MarkedChar3D getPattern() {
        return this.patterns.getOriginal();
    }

    public List<ItemStack> displaySlot(int x, int y, int z) {
        return switch (this.getPattern().get(x, y, z)) {
            case ' ' -> List.of(BlockInput.STACK_ANY);
            case '$' -> List.of(STACK_INPUT.get());
            case '*' -> this.getRitual().getDisplayStacks();
            case '.' -> List.of(BlockInput.STACK_AIR);
            default -> {
                BlockInput input = this.keys.get(this.getPattern().get(x, y, z));
                if (input != null) yield input.getDisplayStacks();
                else yield List.of();
            }
        };
    }

    public boolean isVirtualRender(int x, int y, int z) {
        char c = this.getPattern().get(x, y, z);
        return c == ' ' || c == '$' || c == '.' || (c != '*' && Objects.requireNonNull(this.keys.get(c)).isVirtualRender());
    }

    public void tryBuild(Level level, BlockPos origin) {
        MarkedChar3D pattern = this.getPattern();
        origin = pattern.getOriginInWorld(origin);
        for (int x = 0; x < pattern.xSize(); x++) {
            for (int y = 0; y < pattern.ySize(); y++) {
                for (int z = 0; z < pattern.zSize(); z++) {
                    char key = pattern.get(x, y, z);
                    if (key == '.') {
                        level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    } else if (key == '$') {
                        level.setBlockAndUpdate(origin.offset(x, y, z), CropariaBlocks.PLACEHOLDER.get().defaultBlockState());
                    } else if (key != ' ' && key != '*') {
                        level.setBlockAndUpdate(origin.offset(x, y, z), this.getKeys().get(key).getExampleState());
                    }
                }
            }
        }
    }

    public Vec3i size() {
        return this.getPattern().size();
    }

    @Override
    public TypedSerializer<? extends RitualStructure> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    public boolean matches(RitualStructureContainer matcher, Level level) {
        return this.getRitual().matches(matcher.getState());
    }

    @Override
    @Deprecated
    public @NotNull ItemStack assemble(RitualStructureContainer recipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    public static Result result(List<BlockState> states, @NotNull Runnable destroyAction) {
        return new Result(states, destroyAction);
    }

    public static class Result {
        public static final Result FAIL = new Result(List.of(), () -> {
        });

        @NotNull
        private final List<BlockState> states;
        @NotNull
        private final Runnable destroyAction;

        public Result(@NotNull List<BlockState> states, @NotNull Runnable destroyAction) {
            this.states = states;
            this.destroyAction = destroyAction;
        }

        @NotNull
        public List<BlockState> getStates() {
            return states;
        }

        public void destroy() {
            destroyAction.run();
        }

        public void ifSuccessOrElse(Consumer<Result> state, Runnable onFail) {
            if (this.getStates().isEmpty()) {
                onFail.run();
            } else {
                state.accept(this);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RitualStructure structure)) return false;
        return Objects.equals(ritual, structure.ritual) && Objects.equals(keys, structure.keys) && Objects.equals(patterns, structure.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ritual, keys, patterns);
    }
}
