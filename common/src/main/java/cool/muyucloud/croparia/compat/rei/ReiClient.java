package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.category.*;
import cool.muyucloud.croparia.compat.rei.util.ReiDisplay;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;

import java.util.ArrayList;

public class ReiClient implements REIClientPlugin {
    public static final ArrayList<ReiCategory<?>> CATEGORIES = new ArrayList<>();

    static {
        CATEGORIES.add(new ReiInfusorRecipe());
        CATEGORIES.add(new ReiRitualRecipe());
        CATEGORIES.add(new ReiRitualStructure());
        CATEGORIES.add(new ReiSoakRecipe());
    }

    public void registerCategories(CategoryRegistry registry) {
        CropariaIf.LOGGER.debug("Registering rei recipe categories...");
        CATEGORIES.forEach(category -> {
            registry.add(category);
            registry.addWorkstations(category.getCategoryIdentifier(), category.stations());
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        CropariaIf.LOGGER.debug("Skipping REI recipe display registration during core 1.21.11 migration");
    }
}
