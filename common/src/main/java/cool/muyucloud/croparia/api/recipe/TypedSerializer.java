package cool.muyucloud.croparia.api.recipe;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.access.RecipeManagerAccess;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.registry.Recipes;
import cool.muyucloud.croparia.util.supplier.Mappable;
import dev.architectury.platform.Platform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * An all-in-one recipe utility combined with serialization, typing, syncing and searching.
 */
@SuppressWarnings("unused")
public class TypedSerializer<R extends DisplayableRecipe<?>> {
    public static final Codec<TypedSerializer<?>> CODEC = Identifier.CODEC.xmap(Recipes::find, TypedSerializer::getId);
    public static final Predicate<RecipeHolder<?>> REI = holder -> Platform.isModLoaded("roughlyenoughitems");
    public static final Predicate<RecipeHolder<?>> JEI = holder -> Platform.isModLoaded("jei");
    public static final Predicate<RecipeHolder<?>> NEVER = holder -> false;
    public static final Predicate<RecipeHolder<?>> ALWAYS = holder -> true;
    public static final List<Runnable> AFTER_SYNC_EVENT = new ArrayList<>();

    private final Identifier id;
    private final List<Mappable<ItemStack>> stations;
    private final Class<? extends R> recipeClass;
    private final RecipeType<?> recipeType;
    private final Boolean registerType;
    private final RecipeSerializer<R> serializer;
    private final Predicate<RecipeHolder<?>> syncFilter;
    private transient @Nullable List<RecipeHolder<R>> syncCache;

    /**
     * Create a TypedSerializer with full direct arguments.
     *
     * @param id          Identifier of the recipe type
     * @param recipeClass Class object of the recipe
     * @param serializer  Serializer of the recipe holding the {@link Codec} & {@link StreamCodec}
     * @param syncFilter  A predicate that determines which recipes will be synced to client
     * @param stations    Stations where the recipe would work on
     */
    protected TypedSerializer(@NonNull Identifier id, @NonNull Class<? extends R> recipeClass, final @NonNull RecipeSerializer<R> serializer,
                              final @Nullable RecipeType<?> recipeType, @NonNull Boolean registerType,
                              final Predicate<RecipeHolder<?>> syncFilter, List<Mappable<ItemStack>> stations) {
        this.id = id;
        this.stations = stations;
        this.recipeClass = recipeClass;
        this.recipeType = recipeType;
        this.registerType = registerType;
        this.serializer = serializer;
        this.syncFilter = syncFilter;
    }

    public RecipeType<?> getRecipeType() {
        return recipeType;
    }

    public boolean shouldRegisterType() {
        return registerType;
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
    public void syncRecipes(Collection<RecipeHolder<R>> recipes) {
        this.syncCache = ImmutableList.copyOf(recipes.stream().filter(
            holder -> this.shouldSync((RecipeHolder<DisplayableRecipe<RecipeInput>>) holder)
        ).collect(ImmutableList.toImmutableList()));
    }

    public List<R> find() {
        return this.findHolders().stream().map(RecipeHolder::value).toList();
    }

    @SuppressWarnings("unchecked")
    public <I extends RecipeInput> Optional<R> find(I input, Level level) {
        for (R recipe : this.find()) {
            if (((Recipe<I>) recipe).matches(input, level)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<RecipeHolder<R>> findHolders() {
        List<RecipeHolder<R>> holders = new ArrayList<>();
        CropariaIf.ifServerOrElse(server -> holders.addAll(
            ((RecipeManagerAccess) server.getRecipeManager()).cif$byType((RecipeType) this.adapt().getRecipeType())
                .stream().map(holder -> (RecipeHolder<R>) holder).toList()
        ), () -> {
            if (this.syncCache != null) {
                holders.addAll(this.syncCache);
            }
        });
        return holders;
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

    public boolean isClientSynchronized() {
        return this.syncFilter != NEVER;
    }

    public <I extends RecipeInput, R1 extends DisplayableRecipe<I>> boolean shouldSync(RecipeHolder<R1> holder) {
        return holder.value().getTypedSerializer() == this && this.syncFilter.test(holder);
    }

    public @NonNull RecipeSerializer<R> getSerializer() {
        return this.serializer;
    }

    public @NonNull MapCodec<R> codec() {
        return this.getSerializer().codec();
    }

    @NonNull
    @SuppressWarnings("deprecation")
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return this.getSerializer().streamCodec();
    }

    public Identifier getId() {
        return id;
    }

    public static <R extends DisplayableRecipe<?>> Builder<R> builder() {
        return new Builder<>();
    }

    public static class Builder<R extends DisplayableRecipe<?>> {
        private Identifier id;
        private final @NonNull List<Mappable<ItemStack>> stations = new ArrayList<>();
        private Class<? extends R> recipeClass;
        private RecipeType<?> recipeType;
        private Boolean registerType;
        private RecipeSerializer<R> serializer;
        private @NonNull Predicate<RecipeHolder<?>> syncFilter = NEVER;

        public TypedSerializer<R> build() {
            assert id != null;
            assert recipeClass != null;
            assert recipeType != null;
            assert serializer != null;
            assert syncFilter != null;
            return new TypedSerializer<>(id, recipeClass, serializer, recipeType, registerType, syncFilter, stations);
        }

        public Builder<R> id(@NonNull Identifier id) {
            this.id = id;
            return this;
        }

        public Builder<R> recipeClass(@NonNull Class<? extends R> recipeClass) {
            this.recipeClass = recipeClass;
            return this;
        }

        public Builder<R> recipeType() {
            this.recipeType = new DisplayableRecipeType<>(this.id);
            this.registerType = true;
            return this;
        }

        public Builder<R> recipeType(RecipeType<?> recipeType) {
            this.recipeType = recipeType;
            this.registerType = true;
            return this;
        }

        public Builder<R> shouldRegisterType() {
            this.registerType = true;
            return this;
        }

        public Builder<R> cancelRegisterType() {
            this.registerType = false;
            return this;
        }

        public Builder<R> serializer(MapCodec<R> codec) {
            return this.serializer(codec, CodecUtil.toStream(codec.codec()));
        }

        public Builder<R> serializer(MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
            return this.serializer(new RecipeSerializer<>(codec, streamCodec));
        }

        public Builder<R> serializer(RecipeSerializer<R> serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder<R> syncFilter(Predicate<RecipeHolder<?>> syncFilter) {
            this.syncFilter = syncFilter;
            return this;
        }

        public Builder<R> orSyncFilter(Predicate<RecipeHolder<?>> syncFilter) {
            this.syncFilter = holder -> this.syncFilter.test(holder) || syncFilter.test(holder);
            return this;
        }

        public Builder<R> andSyncFilter(Predicate<RecipeHolder<?>> syncFilter) {
            this.syncFilter = holder -> this.syncFilter.test(holder) && syncFilter.test(holder);
            return this;
        }

        @SafeVarargs
        public final Builder<R> withWorkStations(Mappable<ItemStack>... stations) {
            this.stations.addAll(Arrays.asList(stations));
            return this;
        }
    }
}
