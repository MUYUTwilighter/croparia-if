package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.compat.rei.category.InfusorRecipeDisplayCategory;
import cool.muyucloud.croparia.compat.rei.category.RitualRecipeDisplayCategory;
import cool.muyucloud.croparia.compat.rei.category.RitualStructureDisplayCategory;
import cool.muyucloud.croparia.compat.rei.category.SoakRecipeDisplayCategory;
import cool.muyucloud.croparia.compat.rei.display.SimpleCategory;
import cool.muyucloud.croparia.compat.rei.display.SimpleDisplay;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ReiCommon implements REICommonPlugin {
    private static final Set<SimpleCategory<?>> CATEGORIES = new HashSet<>();

    public static void forEach(Consumer<SimpleCategory<? extends DisplayableRecipe<?>>> consumer) {
        CATEGORIES.forEach(consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerDisplays(ServerDisplayRegistry registry) {
        CropariaIf.LOGGER.info("Registering rei recipe fillers...");
        forEach(serializer -> registry.beginRecipeFiller(serializer.getRecipeClass())
            .filterType((RecipeType<DisplayableRecipe<?>>) serializer.getRecipeType())
            .fill(holder -> new SimpleDisplay<>((RecipeHolder<DisplayableRecipe<?>>) holder, (SimpleCategory<DisplayableRecipe<?>>) serializer)));
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        CATEGORIES.add(InfusorRecipeDisplayCategory.INSTANCE);
        CATEGORIES.add(RitualRecipeDisplayCategory.INSTANCE);
        CATEGORIES.add(RitualStructureDisplayCategory.INSTANCE);
        CATEGORIES.add(SoakRecipeDisplayCategory.INSTANCE);
        CropariaIf.LOGGER.info("Registering rei recipe display serializers...");
        forEach(serializer -> registry.register(serializer.getId(), serializer.getSerializer()));
    }
}
