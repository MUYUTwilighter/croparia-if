package cool.muyucloud.croparia.compat.rei.display;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.util.supplier.Mappable;
import cool.muyucloud.croparia.util.text.Texts;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Map;
import java.util.function.Supplier;

public abstract class SimpleCategory<R extends DisplayableRecipe<?>> implements DisplayCategory<SimpleDisplay<R>> {
    private final Class<? extends R> recipeClass;
    private final CategoryIdentifier<SimpleDisplay<R>> categoryIdentifier;
    private final DisplaySerializer<SimpleDisplay<R>> serializer;

    public SimpleCategory() {
        this.recipeClass = this.getRecipeType().getRecipeClass();
        this.categoryIdentifier = CategoryIdentifier.of(this.getRecipeType().getId().orElseThrow());
        this.serializer = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                this.getRecipeType().codec().fieldOf("recipe").forGetter(SimpleDisplay::getRecipe),
                ResourceLocation.CODEC.fieldOf("id").forGetter(SimpleDisplay::getId)
            ).apply(instance, (recipe, id) -> new SimpleDisplay<>(recipe, id, this))),
            StreamCodec.of((buf, display) -> {
                buf.writeJsonWithCodec(this.getRecipeType().codec().codec(), display.getRecipe());
                buf.writeResourceLocation(display.getId());
            }, buf -> {
                R recipe = buf.readJsonWithCodec(this.getRecipeType().codec().codec());
                ResourceLocation id = buf.readResourceLocation();
                return new SimpleDisplay<>(recipe, id, this);
            })
        );
    }

    @Override
    public Component getTitle() {
        return Texts.literal("gui.%s.%s.title".formatted(this.getId().getNamespace(), this.getId().getPath()));
    }

    @Override
    public Renderer getIcon() {
        EntryIngredient[] stations = this.stations();
        if (stations.length > 0) return stations[0].getFirst();
        else throw new RuntimeException("Override is required if no stations are provided for " + this.getRecipeType().getId());
    }

    public abstract TypedSerializer<R> getRecipeType();

    public Class<? extends R> getRecipeClass() {
        return recipeClass;
    }

    public ResourceLocation getId() {
        return getCategoryIdentifier().getIdentifier();
    }

    public DisplaySerializer<SimpleDisplay<R>> getSerializer() {
        return serializer;
    }

    public abstract Map<String, Supplier<EntryIngredient>> inputEntries(RecipeHolder<R> holder);

    public abstract Map<String, Supplier<EntryIngredient>> outputEntries(RecipeHolder<R> holder);

    public EntryIngredient[] stations() {
        EntryIngredient[] array = new EntryIngredient[this.getRecipeType().getStations().size()];
        int i = 0;
        for (Mappable<ItemStack> stack : this.getRecipeType().getStations()) {
            array[i] = EntryIngredients.of(stack.get());
            i++;
        }
        return array;
    }

    @Override
    public CategoryIdentifier<SimpleDisplay<R>> getCategoryIdentifier() {
        return categoryIdentifier;
    }
}
