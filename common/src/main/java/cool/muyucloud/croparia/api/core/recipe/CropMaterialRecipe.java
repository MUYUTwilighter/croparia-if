package cool.muyucloud.croparia.api.core.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.api.recipe.entry.ItemOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CropMaterialRecipe extends CustomRecipe implements DisplayableRecipe<CraftingInput> {
    public static final MapCodec<CropMaterialRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("registry").forGetter(CropMaterialRecipe::getRegistryId),
        Identifier.CODEC.fieldOf("crop").forGetter(CropMaterialRecipe::getCropId),
        ItemOutput.CODEC.optionalFieldOf("output").forGetter(CropMaterialRecipe::getOutput)
    ).apply(instance, (registry, crop, output) -> new CropMaterialRecipe(registry, crop, output.orElse(null))));
    public static final RecipeSerializer<CropMaterialRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, CodecUtil.toStream(CODEC.codec()));

    public static final TypedSerializer<CropMaterialRecipe> TYPED_SERIALIZER = TypedSerializer.<CropMaterialRecipe>builder()
        .id(CropariaIf.of("crop_material"))
        .serializer(CODEC)
        .recipeClass(CropMaterialRecipe.class)
        .recipeType(RecipeType.CRAFTING)
        .syncFilter(TypedSerializer.JEI).orSyncFilter(TypedSerializer.REI)
        .withWorkStations(Items.CRAFTING_TABLE::getDefaultInstance)
        .cancelRegisterType()
        .build();

    @SuppressWarnings("unchecked")
    public static Optional<AbstractCrop<?>> readCrop(CraftingInput input) {
        final Optional<AbstractCrop<?>> empty = Optional.empty();
        if (input.isEmpty()) return empty;
        List<ItemStack> stacks = input.items();
        if (stacks.size() != 1) return empty;
        return (Optional<AbstractCrop<?>>) (Object) AbstractCrop.readCrop(stacks.getFirst());
    }

    private final @NotNull CropRegistry<?> registry;
    private final @NotNull AbstractCrop<?> crop;
    private final @Nullable ItemOutput output;

    public CropMaterialRecipe(@NotNull Identifier registryId, @NotNull Identifier cropId, @Nullable ItemOutput output) {
        DgRegistry<?> reg = DgRegistry.REGISTRY.get(registryId);
        if (!(reg instanceof CropRegistry<? extends AbstractCrop<?>> cropReg)) {
            throw new IllegalArgumentException("Registry %s is either absent or not a crop registry".formatted(registryId));
        }
        Optional<? extends AbstractCrop<?>> mayCrop = cropReg.getById(cropId);
        if (mayCrop.isEmpty()) {
            throw new IllegalArgumentException("Crop %s is not found in registry %s".formatted(cropId, registryId));
        }
        this(cropReg, mayCrop.get(), output);
    }

    public CropMaterialRecipe(@NotNull CropRegistry<?> registry, @NotNull AbstractCrop<?> crop, @Nullable ItemOutput output) {
        this.registry = registry;
        this.crop = crop;
        this.output = output;
    }

    public @NotNull Identifier getRegistryId() {
        return this.getRegistry().getId();
    }

    public @NotNull CropRegistry<?> getRegistry() {
        return registry;
    }

    public @NotNull Identifier getCropId() {
        return this.getCrop().getKey();
    }

    public @NotNull AbstractCrop<?> getCrop() {
        return crop;
    }

    public Optional<ItemOutput> getOutput() {
        return Optional.ofNullable(output);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return readCrop(input).isPresent();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input) {
        Optional<AbstractCrop<?>> mayCrop = readCrop(input);
        if (mayCrop.isEmpty()) return ItemStack.EMPTY;
        if (this.getOutput().isPresent()) {
            return this.getOutput().orElseThrow().createStack();
        } else {
            return mayCrop.orElseThrow().getMaterial().asItem();
        }
    }

    @Override
    public TypedSerializer<? extends DisplayableRecipe<CraftingInput>> getTypedSerializer() {
        return TYPED_SERIALIZER;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.create(Ingredient.of(crop.getFruit().getItem()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull RecipeType<CraftingRecipe> getType() {
        return (RecipeType<CraftingRecipe>) DisplayableRecipe.super.getType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull RecipeSerializer<CropMaterialRecipe> getSerializer() {
        return (RecipeSerializer<CropMaterialRecipe>) DisplayableRecipe.super.getSerializer();
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
