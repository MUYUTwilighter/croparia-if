package cool.muyucloud.croparia.api.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.RecipeManagerAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.supplier.Mappable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
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
    public static final Codec<TypedSerializer<?>> CODEC = ResourceLocation.CODEC.xmap(Recipes::find, TypedSerializer::getId);

    private final ResourceLocation id;
    private final List<Mappable<ItemStack>> stations;
    private final Class<? extends R> recipeClass;
    private final MapCodec<R> codec;

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final MapCodec<R> codec,
                           Mappable<ItemStack>... stations) {
        this.id = id;
        this.stations = new ArrayList<>();
        this.stations.addAll(Arrays.asList(stations));
        this.recipeClass = recipeClass;
        this.codec = codec;
    }

    /**
     * Adapt this serializer to a more specific type.
     *
     * @apiNote CHECK TYPE SAFETY BEFORE USE!
     */
    @SuppressWarnings("unchecked")
    public <C extends Container, T extends DisplayableRecipe<C>> TypedSerializer<T> adapt() {
        return (TypedSerializer<T>) this;
    }

    @SuppressWarnings("unchecked")
    public List<R> find() {
        List<R> recipes = new ArrayList<>();
        CropariaIf.ifServerOrElse(server -> {
            List<?> found = new ArrayList<>(((RecipeManagerAccess) server.getRecipeManager()).cif$byType(this.adapt()));
            found.forEach(recipe -> recipes.add((R) recipe));
        }, () -> {
            Level level = getClientLevel();
            if (level == null) return;
            RecipeManagerAccess access = (RecipeManagerAccess) level.getRecipeManager();
            access.cif$byType(this.adapt()).forEach(recipe -> recipes.add((R) recipe));
        });
        return recipes;
    }

    @SuppressWarnings("unchecked")
    public <C extends Container> Optional<R> find(C input, Level level) {
        return level.getRecipeManager().getRecipeFor(this.adapt(), input, level).map(recipe -> (R) recipe);
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

    public ResourceLocation getId() {
        return id;
    }

    public @NotNull MapCodec<R> getCodec() {
        return codec;
    }

    @Override
    public @NotNull R fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        R recipe = CodecUtil.getOrThrow(CodecUtil.decodeJson(json, this.codec.codec()), IllegalArgumentException::new);
        recipe.setId(recipeId);
        return recipe;
    }

    @Override
    public R fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        if (tag == null) {
            throw new IllegalArgumentException("Missing recipe payload for " + recipeId);
        }
        R recipe = CodecUtil.getOrThrow(this.codec.codec().parse(NbtOps.INSTANCE, tag), IllegalArgumentException::new);
        recipe.setId(recipeId);
        return recipe;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull R recipe) {
        CompoundTag tag = CodecUtil.getOrThrow(this.codec.codec().encodeStart(NbtOps.INSTANCE, recipe), IllegalArgumentException::new)
            instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
        buffer.writeNbt(tag);
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
