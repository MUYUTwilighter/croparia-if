package cool.muyucloud.croparia.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.RecipeManagerAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.supplier.Mappable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class TypedSerializer<R extends DisplayableRecipe<?>> implements RecipeType<R>, RecipeSerializer<R> {
    public static final Codec<TypedSerializer<?>> CODEC = Identifier.CODEC.xmap(Recipes::find, TypedSerializer::getId);

    private final Identifier id;
    private final List<Mappable<ItemStack>> stations;
    private final Class<? extends R> recipeClass;
    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

    @SafeVarargs
    public TypedSerializer(Identifier id, Class<? extends R> recipeClass, final MapCodec<R> codec,
                           Mappable<ItemStack>... stations) {
        this(id, recipeClass, codec, CodecUtil.toStream(codec.codec()), stations);
    }

    @SafeVarargs
    public TypedSerializer(Identifier id, Class<? extends R> recipeClass, final MapCodec<R> codec,
                           final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec,
                           Mappable<ItemStack>... stations) {
        this.id = id;
        this.stations = new ArrayList<>();
        this.stations.addAll(Arrays.asList(stations));
        this.recipeClass = recipeClass;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    /**
     * Adapt this serializer to a more specific type.
     *
     * @apiNote CHECK TYPE SAFETY BEFORE USE!
     */
    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends DisplayableRecipe<I>> TypedSerializer<T> adapt() {
        return (TypedSerializer<T>) this;
    }

    @SuppressWarnings("unchecked")
    public List<R> find() {
        List<R> recipes = new ArrayList<>();
        CropariaIf.ifServerOrElse(server -> recipes.addAll(
            ((RecipeManagerAccess) server.getRecipeManager()).cif$byType(this.adapt())
                .stream().map(holder -> (R) holder.value()).toList()
        ), () -> {
            Level level = getClientLevel();
            if (level == null) return;
            RecipeManagerAccess access = (RecipeManagerAccess) level.recipeAccess();
            access.cif$byType(this.adapt()).forEach(holder -> recipes.add((R) holder.value()));
        });
        return recipes;
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput> Optional<R> find(I input, Level level) {
        return ((RecipeManager) level.recipeAccess()).getRecipeFor(this.adapt(), input, level).map(
            holder -> (R) holder.value()
        );
    }

    public List<Mappable<ItemStack>> getStations() {
        return stations;
    }

    public void addStation(Mappable<ItemStack> station) {
        this.stations.add(station);
    }

    public Class<? extends R> getRecipeClass() {
        return recipeClass;
    }

    @Override
    public @NotNull MapCodec<R> codec() {
        return codec;
    }

    @Override
    @NotNull
    @SuppressWarnings("deprecation")
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }

    public Identifier getId() {
        return id;
    }

    @Nullable
    private static Level getClientLevel() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Field levelField = minecraftClass.getField("level");
            Object level = levelField.get(minecraft);
            return level instanceof Level typedLevel ? typedLevel : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
