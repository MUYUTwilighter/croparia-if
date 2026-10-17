package cool.muyucloud.croparia.api.core.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CropMaterialRecipe extends CustomRecipe {
    public static final MapCodec<CropMaterialRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("registry").forGetter(CropMaterialRecipe::getRegistryId),
        Identifier.CODEC.fieldOf("crop").forGetter(CropMaterialRecipe::getCropId),
        ItemOutput.CODEC.optionalFieldOf("output").forGetter(CropMaterialRecipe::getOutput)
    ).apply(instance, (registry, crop, output) -> new CropMaterialRecipe(registry, crop, output.orElse(null))));
    public static final RecipeSerializer<CropMaterialRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, CodecUtil.toStream(CODEC.codec()));

    public static Optional<AbstractCrop<?>> readCrop(CraftingInput input) {
        final Optional<AbstractCrop<?>> empty = Optional.empty();
        if (input.isEmpty()) return empty;
        List<ItemStack> stacks = input.items();
        if (stacks.size() != 1) return empty;
        if (!(stacks.getFirst().getItem() instanceof CropAccess<?> access)) return empty;
        return Optional.of(access.getCrop());
    }

    private final @NonNull CropRegistry<?> registry;
    private final @NonNull AbstractCrop<?> crop;
    private final @Nullable ItemOutput output;

    public CropMaterialRecipe(@NonNull Identifier registryId, @NonNull Identifier cropId, @Nullable ItemOutput output) {
        DgRegistry<?> reg = DgRegistry.REGISTRY.get(registryId);
        if (!(reg instanceof CropRegistry<? extends AbstractCrop<?>> cropReg)) {
            throw new IllegalArgumentException("Registry %s is either absent or not a crop registry".formatted(registryId));
        }
        Optional<? extends AbstractCrop<?>> mayCrop = cropReg.forName(cropId);
        if (mayCrop.isEmpty()) {
            throw new IllegalArgumentException("Crop %s is not found in registry %s".formatted(cropId, registryId));
        }
        this(cropReg, mayCrop.get(), output);
    }

    public CropMaterialRecipe(@NonNull CropRegistry<?> registry, @NonNull AbstractCrop<?> crop, @Nullable ItemOutput output) {
        this.registry = registry;
        this.crop = crop;
        this.output = output;
    }

    public @NonNull Identifier getRegistryId() {
        return this.getRegistry().getId();
    }

    public @NonNull CropRegistry<?> getRegistry() {
        return registry;
    }

    public @NonNull Identifier getCropId() {
        return this.getCrop().getKey();
    }

    public @NonNull AbstractCrop<?> getCrop() {
        return crop;
    }

    public Optional<ItemOutput> getOutput() {
        return Optional.ofNullable(output);
    }

    @Override
    public boolean matches(@NonNull CraftingInput input, @NonNull Level level) {
        return readCrop(input).isPresent();
    }

    @Override
    public @NonNull ItemStack assemble(@NonNull CraftingInput input) {
        Optional<AbstractCrop<?>> mayCrop = readCrop(input);
        if (mayCrop.isEmpty()) return ItemStack.EMPTY;
        if (this.getOutput().isPresent()) {
            return this.getOutput().orElseThrow().createStack();
        } else {
            return mayCrop.orElseThrow().getMaterial().asItem();
        }
    }

    @Override
    public @NonNull RecipeSerializer<CropMaterialRecipe> getSerializer() {
        return SERIALIZER;
    }
}
