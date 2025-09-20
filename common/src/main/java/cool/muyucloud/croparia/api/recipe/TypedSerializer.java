package cool.muyucloud.croparia.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.RecipeManagerAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncClear;
import cool.muyucloud.croparia.api.recipe.network.S2CSyncRecipe;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.Ref;
import cool.muyucloud.croparia.util.SidedRef;
import cool.muyucloud.croparia.util.supplier.Mappable;
import dev.architectury.platform.Platform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class TypedSerializer<R extends DisplayableRecipe<?>>
    extends RecipeBookCategory implements RecipeType<R>, RecipeSerializer<R> {
    public static final Codec<TypedSerializer<?>> CODEC = ResourceLocation.CODEC.xmap(Recipes::find, TypedSerializer::getId);

    public static Predicate<RecipeHolder<?>> JEI = holder -> Platform.isModLoaded("jei") && !Platform.isModLoaded("emi");
    public static Predicate<RecipeHolder<?>> NEVER = holder -> false;
    public static Predicate<RecipeHolder<?>> ALWAYS = holder -> true;

    private final ResourceLocation id;
    private final List<Mappable<ItemStack>> stations;
    private final Class<? extends R> recipeClass;
    private final Codec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;
    private final transient RecipeDisplay.Type<R> displayType;
    private final transient Predicate<RecipeHolder<?>> syncFilter;
    private final transient Collection<R> synced = new HashSet<>();

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final MapCodec<R> codec, Predicate<RecipeHolder<?>> syncFilter,
                           Mappable<ItemStack>... stations) {
        this(id, recipeClass, codec.codec(), CodecUtil.toStream(codec), syncFilter, stations);
    }

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final MapCodec<R> codec,
                           final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec, Predicate<RecipeHolder<?>> syncFilter,
                           Mappable<ItemStack>... stations) {
        this(id, recipeClass, codec.codec(), streamCodec, syncFilter, stations);
    }

    @SafeVarargs
    @SuppressWarnings("unused")
    public TypedSerializer(ResourceLocation id, Class<? extends R> clazz, final Codec<R> codec, Predicate<RecipeHolder<?>> syncFilter,
                           Mappable<ItemStack>... stations) {
        this(id, clazz, codec, CodecUtil.toStream(codec), syncFilter, stations);
    }

    @SafeVarargs
    public TypedSerializer(ResourceLocation id, Class<? extends R> recipeClass, final Codec<R> codec,
                           final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec, Predicate<RecipeHolder<?>> syncFilter,
                           Mappable<ItemStack>... stations) {
        this.id = id;
        this.stations = new ArrayList<>();
        this.stations.addAll(Arrays.asList(stations));
        this.recipeClass = recipeClass;
        this.codec = codec;
        this.streamCodec = streamCodec;
        this.syncFilter = syncFilter;
        this.displayType = new RecipeDisplay.Type<>(CodecUtil.toMap(codec), streamCodec);
    }

    public <I extends RecipeInput, T extends DisplayableRecipe<I>> boolean shouldSync(RecipeHolder<T> holder) {
        if (holder.value().getTypedSerializer() == this) {
            return this.syncFilter.test(holder);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends DisplayableRecipe<I>> TypedSerializer<T> adapt() {
        return (TypedSerializer<T>) this;
    }

    @SuppressWarnings("unchecked")
    public List<R> find() {
        List<R> recipes = new ArrayList<>();
        SidedRef.ifServerOrElse(
            () -> CropariaIf.ifServer(server -> recipes.addAll(
                ((RecipeManagerAccess) server.getRecipeManager()).cif$byType(this.adapt()).stream().map(holder -> (R) holder.value()).toList()
            )),
            () -> recipes.addAll(this.getSyncedRecipes())
        );
        return recipes;
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput> Optional<R> find(I input, Level level) {
        Ref<R> result = new Ref<>();
        SidedRef.ifServerOrElse(() -> CropariaIf.ifServer(
            server -> result.set((R) server.getRecipeManager().getRecipeFor(this.adapt(), input, level).map(RecipeHolder::value).orElse(null))
        ), () -> {
            TypedSerializer<? extends DisplayableRecipe<I>> adapted = this.adapt();
            for (DisplayableRecipe<I> recipe : adapted.synced) {
                if (recipe.matches(input, level)) result.set((R) recipe);
            }
        });
        return result.optional();
    }

    public void syncRecipes() {
        CropariaIf.ifServer(server -> {
            S2CSyncClear.of(this).send();
            ((RecipeManagerAccess) server.getRecipeManager()).cif$byType(this.adapt()).forEach(holder -> {
                if (this.shouldSync(holder)) {
                    SidedRef.ifClient(() -> this.adapt().recordRecipe(holder.value()));
                    S2CSyncRecipe.of(holder.value()).send();
                }
            });
        });
    }

    public void syncRecipes(@NotNull ServerPlayer player) {
        CropariaIf.ifServer(server -> {
            S2CSyncClear.of(this).send(player);
            ((RecipeManagerAccess) server.getRecipeManager()).cif$byType(this.adapt()).forEach(holder -> {
                if (this.shouldSync(holder)) {
                    SidedRef.ifClient(() -> this.adapt().recordRecipe(holder.value()));
                    S2CSyncRecipe.of(holder.value()).send(player);
                }
            });
        });
    }

    public Collection<R> getSyncedRecipes() {
        return this.synced;
    }

    public void recordRecipe(R recipe) {
        this.synced.add(recipe);
    }

    public void syncClear() {
        this.synced.clear();
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
        return CodecUtil.toMap(codec);
    }

    @Override
    @NotNull
    @SuppressWarnings("deprecation")
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
