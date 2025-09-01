package cool.muyucloud.croparia.api.core.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.container.RitualStructureContainer;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.BlockInput;
import cool.muyucloud.croparia.api.recipe.structure.Char3D;
import cool.muyucloud.croparia.api.recipe.structure.MarkedChar3D;
import cool.muyucloud.croparia.api.recipe.structure.MarkedTransformableChar3D;
import cool.muyucloud.croparia.registry.CropariaItems;
import cool.muyucloud.croparia.util.Constants;
import cool.muyucloud.croparia.util.codec.CodecUtil;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class RitualStructure implements DisplayableRecipe<RitualStructureContainer> {
    public static final TypedSerializer<RitualStructure> TYPED_SERIALIZER = new TypedSerializer<>(
        CropariaIf.of("ritual_structure"), RitualStructure.class,
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(CodecUtil.CHAR, BlockInput.CODEC).fieldOf("keys").forGetter(RitualStructure::getKeys),
            Char3D.CODEC.fieldOf("pattern").forGetter(RitualStructure::getPattern)
        ).apply(instance, RitualStructure::new)),
        Mappable.of(CropariaItems.RITUAL_STAND, Item::getDefaultInstance),
        Mappable.of(CropariaItems.RITUAL_STAND_2, Item::getDefaultInstance),
        Mappable.of(CropariaItems.RITUAL_STAND_3, Item::getDefaultInstance)
    );
    public static final OnLoadSupplier<Map<RitualStructure, Mappable<ItemStack>>> STRUCTURES = OnLoadSupplier.of(() -> {
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

    @NotNull
    private final ImmutableMap<Character, BlockInput> keys;
    @NotNull
    private final MarkedTransformableChar3D patterns;
    private final transient LazySupplier<List<List<ItemStack>>> inputsCache = LazySupplier.of(() -> {
        ImmutableList.Builder<List<ItemStack>> builder = ImmutableList.builder();
        this.getPattern().forEachChar((key, count) -> {
            if (key == '*') {
                builder.add(List.of(STRUCTURES.get().getOrDefault(this, () -> BlockInput.STACK_UNKNOWN).getOr(BlockInput.STACK_UNKNOWN)));
            } else if (key != '.' && key != ' ' && key != '$') {
                BlockInput input = this.getKeys().get(key);
                if (input != null) builder.add(input.getDisplayStacks().stream().map(stack -> stack.copyWithCount(count)).toList());
                else builder.add(List.of());
            }
        });
        return builder.build();
    });

    public RitualStructure(@NotNull Map<Character, BlockInput> keyDeclarations, Char3D rawPattern) {
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

    @Override
    public @NotNull List<List<ItemStack>> getInputs() {
        return this.inputsCache.get();
    }

    public @Nullable BlockState matchTransformed(BlockPos origin, Level level, Char3D pattern, BlockState ritualBlock, boolean destroy) {
        List<BlockPos> inputPositions = new LinkedList<>();
        BlockState inputBlock = null;
        for (int x = 0; x < pattern.maxX(); x++) {
            for (int y = 0; y < pattern.maxY(); y++) {
                for (int z = 0; z < pattern.maxZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    char key = pattern.get(x, y, z);
                    if (key == '$') {
                        if (inputBlock != null && !state.equals(inputBlock)) {
                            return null;
                        }
                        inputPositions.add(pos);
                        inputBlock = state;
                    } else if (key == '*') {
                        if (!state.equals(ritualBlock)) {
                            return null;
                        }
                    } else if (key == '.') {
                        if (!state.isAir()) {
                            return null;
                        }
                    } else if (key == ' ') {
                        BlockInput.ANY.matches(state);
                    } else {
                        BlockInput input = this.keys.get(key);
                        if (input == null || !input.matches(state)) {
                            return null;
                        }
                    }
                }
            }
        }
        if (destroy) {
            for (BlockPos pos : inputPositions) {
                level.destroyBlock(pos, false);
            }
        }
        return inputBlock;
    }

    public Optional<BlockState> matches(BlockPos ritualPos, Level level) {
        BlockState ritualBlock = level.getBlockState(ritualPos);
        for (MarkedChar3D pattern : patterns) {
            BlockState inputBlock = matchTransformed(pattern.getOriginInWorld(ritualPos), level, pattern, ritualBlock, false);
            if (inputBlock != null) {
                return Optional.of(inputBlock);
            }
        }
        return Optional.empty();
    }

    public Optional<BlockState> matchesAndDestroy(BlockPos ritualPos, Level level) {
        BlockState ritualBlock = level.getBlockState(ritualPos);
        for (MarkedChar3D pattern : patterns) {
            BlockState inputBlock = matchTransformed(pattern.getOriginInWorld(ritualPos), level, pattern, ritualBlock, true);
            if (inputBlock != null) {
                return Optional.of(inputBlock);
            }
        }
        return Optional.empty();
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
            case '*' ->
                List.of(STRUCTURES.get().getOrDefault(this, () -> BlockInput.STACK_UNKNOWN).getOr(BlockInput.STACK_UNKNOWN));
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
        return c == ' ' || c == '$' || c == '.' || Objects.requireNonNull(this.keys.get(c)).isVirtualRender();
    }

    public Vec3i size() {
        return this.getPattern().size();
    }

    @Override
    public TypedSerializer<? extends RitualStructure> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    @Deprecated
    public boolean matches(RitualStructureContainer recipeInput, Level level) {
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(RitualStructureContainer recipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }
}
