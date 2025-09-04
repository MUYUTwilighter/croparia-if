package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Recipes {
    private static final Map<ResourceLocation, TypedSerializer<?>> TYPES = new HashMap<>();
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(CropariaIf.MOD_ID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(CropariaIf.MOD_ID, Registries.RECIPE_SERIALIZER);
    private static final DeferredRegister<RecipeDisplay.Type<?>> RECIPE_DISPLAYS = DeferredRegister.create(CropariaIf.MOD_ID, Registries.RECIPE_DISPLAY);
    private static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORIES = DeferredRegister.create(CropariaIf.MOD_ID, Registries.RECIPE_BOOK_CATEGORY);

    public static final TypedSerializer<InfusorRecipe> INFUSOR = register(InfusorRecipe.TYPED_SERIALIZER);
    @SuppressWarnings("unused")
    public static final TypedSerializer<InfusorRecipe> INFUSOR_OLD = register(InfusorRecipe.OLD_TYPED_SERIALIZER);
    public static final TypedSerializer<RitualRecipe> RITUAL = register(RitualRecipe.TYPED_SERIALIZER);
    public static final TypedSerializer<RitualStructure> RITUAL_STRUCTURE = register(RitualStructure.TYPED_SERIALIZER);
    @SuppressWarnings("unused")
    public static final TypedSerializer<SoakRecipe> SOAK = register(SoakRecipe.TYPED_SERIALIZER);

    public static <R extends DisplayableRecipe<?>> TypedSerializer<R> register(TypedSerializer<R> typedSerializer) {
        TYPES.put(typedSerializer.getId(), typedSerializer);
        RECIPE_TYPES.register(typedSerializer.getId(), () -> typedSerializer);
        RECIPE_SERIALIZERS.register(typedSerializer.getId(), () -> typedSerializer);
        RECIPE_BOOK_CATEGORIES.register(typedSerializer.getId(), () -> typedSerializer);
        RECIPE_DISPLAYS.register(typedSerializer.getId(), typedSerializer::displayType);
        return typedSerializer;
    }

    public static void forEach(Consumer<TypedSerializer<?>> consumer) {
        TYPES.forEach((id, supplier) -> consumer.accept(supplier));
    }

    public static TypedSerializer<?> find(ResourceLocation id) {
        return TYPES.get(id);
    }

    public static void register() {
        RECIPE_TYPES.register();
        RECIPE_SERIALIZERS.register();
        RECIPE_DISPLAYS.register();
        RECIPE_BOOK_CATEGORIES.register();
    }
}
