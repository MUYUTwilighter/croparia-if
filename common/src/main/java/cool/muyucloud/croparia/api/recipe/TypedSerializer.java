package cool.muyucloud.croparia.api.recipe;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.util.codec.CodecUtil;
import cool.muyucloud.croparia.util.supplier.Mappable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.jetbrains.annotations.NotNull;

public class TypedSerializer<R extends DisplayableRecipe<? extends RecipeInput>>
    extends RecipeBookCategory implements RecipeType<R>, RecipeSerializer<R> {
    private final ResourceLocation id;
    private final ImmutableList<Mappable<ItemStack>> stations;
    private final Class<? extends R> recipeClass;
    private final Codec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;
    private transient final RecipeDisplay.Type<R> displayType;

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final MapCodec<R> codec, Mappable<ItemStack>... stations) {
        this(id, recipeClass, codec.codec(), CodecUtil.toStream(codec), stations);
    }

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final MapCodec<R> codec,
                           final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec, Mappable<ItemStack>... stations) {
        this(id, recipeClass, codec.codec(), streamCodec, stations);
    }

    @SafeVarargs
    @SuppressWarnings("unused")
    public TypedSerializer(ResourceLocation id, Class<? extends R> clazz, final Codec<R> codec, Mappable<ItemStack>... stations) {
        this(id, clazz, codec, CodecUtil.toStream(codec), stations);
    }

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final Codec<R> codec,
                           final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec, Mappable<ItemStack>... stations) {
        this.id = id;
        this.stations = ImmutableList.copyOf(stations);
        this.recipeClass = recipeClass;
        this.codec = codec;
        this.streamCodec = streamCodec;
        this.displayType = new RecipeDisplay.Type<>(CodecUtil.toMap(codec), streamCodec);
    }

    public ImmutableList<Mappable<ItemStack>> getStations() {
        return stations;
    }

    public Class<? extends R> getRecipeClass() {
        return recipeClass;
    }

    @Override
    public @NotNull MapCodec<R> codec() {
        return CodecUtil.toMap(codec);
    }

    @Override
    @NotNull
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }

    @NotNull
    public RecipeDisplay.Type<R> displayType() {
        return displayType;
    }

    public ResourceLocation getId() {
        return id;
    }
}
