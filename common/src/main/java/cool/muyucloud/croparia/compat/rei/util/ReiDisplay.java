package cool.muyucloud.croparia.compat.rei.util;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ReiDisplay<R extends DisplayableRecipe<?>> implements Display {
    public static <R extends DisplayableRecipe<?>> DisplaySerializer<ReiDisplay<R>> serializer(ProxyCategory<R> category) {
        return DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                category.getType().codec().fieldOf("recipe").forGetter(ReiDisplay::getRecipe),
                ResourceLocation.CODEC.fieldOf("id").forGetter(ReiDisplay::getId)
            ).apply(instance, (recipe, id) -> new ReiDisplay<>(recipe, id, category))),
            StreamCodec.of((buf, display) -> {
                category.getType().streamCodec().encode(buf, display.getRecipe());
                buf.writeResourceLocation(display.getId());
            }, buf -> {
                R recipe = category.getType().streamCodec().decode(buf);
                ResourceLocation id = buf.readResourceLocation();
                return new ReiDisplay<>(recipe, id, category);
            })
        );
    }

    private final R recipe;
    private final ResourceLocation id;
    private final ProxyCategory<R> category;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public ReiDisplay(RecipeHolder<R> holder, ProxyCategory<R> category) {
        this.recipe = holder.value();
        this.id = holder.id().location();
        this.category = category;
        this.inputs = this.recipe.getInputs().stream().map(EntryIngredients::ofItemStacks).toList();
        this.outputs = this.recipe.getOutputs().stream().map(EntryIngredients::ofItemStacks).toList();
    }

    public ReiDisplay(R recipe, ResourceLocation id, ProxyCategory<R> category) {
        this(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe), category);
    }

    public R getRecipe() {
        return recipe;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<ReiDisplay<R>> getCategoryIdentifier() {
        return this.category.getId();
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(getId());
    }

    @Override
    public @Nullable DisplaySerializer<ReiDisplay<R>> getSerializer() {
        return category.getSerializer();
    }
}
